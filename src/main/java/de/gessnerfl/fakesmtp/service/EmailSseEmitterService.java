package de.gessnerfl.fakesmtp.service;

import de.gessnerfl.fakesmtp.model.Email;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EmailSseEmitterService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Logger logger;

    @Autowired
    public EmailSseEmitterService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Add a new SSE emitter to the list of connected clients
     *
     * @param emitter The SseEmitter to add
     * @return The same SseEmitter for method chaining
     */
    public SseEmitter add(SseEmitter emitter) {
        this.emitters.add(emitter);

        emitter.onCompletion(() -> {
            this.emitters.remove(emitter);
            logger.debug("SSE emitter completed and removed. Active connections: {}", this.emitters.size());
        });

        emitter.onTimeout(() -> {
            emitter.complete();
            this.emitters.remove(emitter);
            logger.debug("SSE emitter timed out and removed. Active connections: {}", this.emitters.size());
        });

        emitter.onError(e -> {
            emitter.complete();
            this.emitters.remove(emitter);
            logger.warn("SSE emitter failed and removed. Active connections: {}", this.emitters.size(), e);
        });

        logger.debug("SSE emitter added. Active connections: {}", this.emitters.size());
        return emitter;
    }

    /**
     * Send an event to all connected clients when a new email is received
     *
     * @param email The newly received email
     */
    public void sendEmailReceivedEvent(Email email) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("email-received")
                        .data(email.getId()));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                logger.warn("Failed to send event to emitter", e);
            }
        });

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            logger.debug("Removed {} dead emitters. Active connections: {}",
                    deadEmitters.size(), this.emitters.size());
        }
    }
}
