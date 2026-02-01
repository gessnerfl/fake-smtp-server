package de.gessnerfl.fakesmtp.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import de.gessnerfl.fakesmtp.config.WebappSessionProperties;
import de.gessnerfl.fakesmtp.model.Email;
import jakarta.annotation.PreDestroy;

/**
 * Service for managing Server-Sent Events (SSE) connections to notify clients about new emails.
 * <p>
 * This service uses Virtual Threads (Java 21) for efficient concurrent event delivery to multiple
 * clients without blocking. Each client connection is handled independently with a configurable
 * timeout to prevent slow clients from affecting others.
 * <p>
 * Features:
 * <ul>
 *   <li>Heartbeat mechanism (default: 30s) to keep connections alive through proxies</li>
 *   <li>Virtual Thread-based async event delivery with timeout (default: 5s)</li>
 *   <li>Automatic cleanup of dead connections</li>
 *   <li>Thread-safe operations using concurrent collections</li>
 * </ul>
 * 
 * @see WebappSessionProperties for configuration options
 */
@Service
public class EmailSseEmitterService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Map<SseEmitter, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final TaskScheduler taskScheduler;
    private final Logger logger;
    private final int heartbeatIntervalSeconds;
    private final int eventSendTimeoutSeconds;
    private final ExecutorService executor;

    @Autowired
    public EmailSseEmitterService(TaskScheduler taskScheduler, Logger logger, WebappSessionProperties sessionProperties) {
        this.taskScheduler = taskScheduler;
        this.logger = logger;
        this.heartbeatIntervalSeconds = sessionProperties.getSseHeartbeatIntervalSeconds();
        this.eventSendTimeoutSeconds = sessionProperties.getSseEventSendTimeoutSeconds();
        // Use Virtual Threads for efficient concurrent I/O operations
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Gracefully shuts down the executor service when the application stops.
     * Waits up to 60 seconds for pending tasks to complete before forcing shutdown.
     */
    @PreDestroy
    public void shutdown() {
        logger.debug("Shutting down SSE emitter service executor");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in time, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor shutdown", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public SseEmitter createEmitter() {
        return createEmitter(3600000L);
    }

    public SseEmitter createEmitter(long timeout) {
        return new SseEmitter(timeout);
    }

    public SseEmitter createAndAddEmitter() {
        return createAndAddEmitter(3600000L);
    }

    public SseEmitter createAndAddEmitter(long timeout) {
        var emitter = new SseEmitter(timeout);
        add(emitter);
        return emitter;
    }

    /**
     * Add a new SSE emitter to the list of connected clients
     *
     * @param emitter The SseEmitter to add
     * @return The same SseEmitter for method chaining
     */
    public SseEmitter add(SseEmitter emitter) {
        this.emitters.add(emitter);
        startHeartbeat(emitter);

        emitter.onCompletion(() -> {
            this.emitters.remove(emitter);
            stopHeartbeat(emitter);
            logger.debug("SSE emitter completed and removed. Active connections: {}", this.emitters.size());
        });

        emitter.onTimeout(() -> {
            emitter.complete();
            this.emitters.remove(emitter);
            stopHeartbeat(emitter);
            logger.debug("SSE emitter timed out and removed. Active connections: {}", this.emitters.size());
        });

        emitter.onError(e -> {
            emitter.complete();
            this.emitters.remove(emitter);
            stopHeartbeat(emitter);
            if (isClientAbort(e)) {
                logger.debug("SSE emitter closed by client. Active connections: {}", this.emitters.size());
            } else {
                logger.warn("SSE emitter failed and removed. Active connections: {}", this.emitters.size(), e);
            }
        });

        logger.debug("SSE emitter added. Active connections: {}", this.emitters.size());
        return emitter;
    }

    private void startHeartbeat(SseEmitter emitter) {
        ScheduledFuture<?> task = taskScheduler.scheduleAtFixedRate(
            () -> sendHeartbeat(emitter),
            Instant.now().plusSeconds(heartbeatIntervalSeconds),
            Duration.ofSeconds(heartbeatIntervalSeconds)
        );
        heartbeatTasks.put(emitter, task);
        logger.debug("Started heartbeat for emitter with interval {}s. Active connections: {}", 
            heartbeatIntervalSeconds, this.emitters.size());
    }

    private void stopHeartbeat(SseEmitter emitter) {
        ScheduledFuture<?> task = heartbeatTasks.remove(emitter);
        if (task != null) {
            task.cancel(false);
            logger.debug("Stopped heartbeat for emitter. Active connections: {}", this.emitters.size());
        }
    }

    private void sendHeartbeat(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                .name("ping")
                .data("keep-alive"));
            logger.debug("Sent heartbeat ping to emitter");
        } catch (IOException e) {
            logger.debug("Failed to send heartbeat - client likely disconnected");
            stopHeartbeat(emitter);
        }
    }

    /**
     * Send an event to all connected clients when a new email is received.
     * <p>
     * This method uses Virtual Threads for concurrent delivery to all clients,
     * with a configurable timeout per client to prevent slow connections from
     * blocking others. Dead emitters are collected and removed in a batch after
     * all send attempts complete.
     *
     * @param email The newly received email
     */
    public void sendEmailReceivedEvent(Email email) {
        if (emitters.isEmpty()) {
            return;
        }

        Set<SseEmitter> deadEmitters = ConcurrentHashMap.newKeySet();
        
        // Send to all clients concurrently using Virtual Threads
        List<CompletableFuture<Void>> futures = emitters.stream()
            .map(emitter -> sendEventAsync(emitter, email, deadEmitters))
            .toList();
        
        // Wait for all sends to complete (with buffer for timeout handling)
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(eventSendTimeoutSeconds + 1, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    logger.debug("Some event sends did not complete in time: {}", ex.getMessage());
                    return null;
                })
                .join();
        } catch (Exception e) {
            logger.debug("Error waiting for event sends to complete: {}", e.getMessage());
        }
        
        // Remove all dead emitters in batch
        if (!deadEmitters.isEmpty()) {
            deadEmitters.forEach(this::stopHeartbeat);
            emitters.removeAll(deadEmitters);
            logger.debug("Removed {} dead emitters. Active connections: {}",
                    deadEmitters.size(), this.emitters.size());
        }
    }
    
    /**
     * Sends an event to a single emitter asynchronously with timeout.
     * 
     * @param emitter The target emitter
     * @param email The email to send
     * @param deadEmitters Set to collect dead emitters
     * @return CompletableFuture representing the send operation
     */
    private CompletableFuture<Void> sendEventAsync(SseEmitter emitter, Email email, Set<SseEmitter> deadEmitters) {
        return CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("email-received")
                        .data(email.getId()));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                if (isClientAbort(e)) {
                    logger.debug("SSE emitter closed by client while sending event.");
                } else {
                    logger.debug("Failed to send event to emitter", e);
                }
            }
        }, executor)
        .orTimeout(eventSendTimeoutSeconds, TimeUnit.SECONDS)
        .exceptionally(ex -> {
            if (ex instanceof TimeoutException) {
                logger.debug("Send timeout after {}s for emitter", eventSendTimeoutSeconds);
            } else {
                logger.debug("Send failed for emitter: {}", ex.getMessage());
            }
            deadEmitters.add(emitter);
            return null;
        });
    }

    private boolean isClientAbort(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String name = throwable.getClass().getName();
        if (name.endsWith("ClientAbortException")) {
            return true;
        }
        String message = throwable.getMessage();
        if (message != null && message.toLowerCase(Locale.ROOT).contains("broken pipe")) {
            return true;
        }
        return isClientAbort(throwable.getCause());
    }
}
