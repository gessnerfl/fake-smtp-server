package de.gessnerfl.fakesmtp.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryRateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryRateLimiter.class);
    
    private final RateLimitingProperties properties;
    private final WebappAuthenticationProperties authProperties;
    private final Map<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleanupExecutor;
    
    public InMemoryRateLimiter(RateLimitingProperties properties, WebappAuthenticationProperties authProperties) {
        this.properties = properties;
        this.authProperties = authProperties;
    }
    
    @PostConstruct
    public void init() {
        if (isRateLimitingActive()) {
            cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "rate-limit-cleanup");
                thread.setDaemon(true);
                return thread;
            });
            
            long cleanupInterval = properties.getCleanupIntervalMinutes();
            cleanupExecutor.scheduleAtFixedRate(
                this::cleanup,
                cleanupInterval,
                cleanupInterval,
                TimeUnit.MINUTES
            );
            
            logger.info("Rate limiter initialized with cleanup interval of {} minutes", cleanupInterval);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Rate limiter cleanup executor shut down");
        }
    }
    
    public boolean isAllowed(String ip) {
        if (!isRateLimitingActive()) {
            return true;
        }
        
        if (isLocalhost(ip) && properties.isWhitelistLocalhost()) {
            return true;
        }
        
        Instant now = Instant.now();
        RateLimitEntry entry = getActiveEntry(ip, now);
        if (entry == null) {
            return true;
        }

        if (entry.isBlocked()) {
            return false;
        }
        
        return entry.getAttemptCount().get() < properties.getMaxAttempts();
    }
    
    public FailedAttemptResult recordFailedAttempt(String ip) {
        if (!isRateLimitingActive() || (isLocalhost(ip) && properties.isWhitelistLocalhost())) {
            return new FailedAttemptResult(false, Integer.MAX_VALUE, 0);
        }

        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(properties.getWindowMinutes() * 60L);
        AtomicInteger attemptCount = new AtomicInteger();
        AtomicInteger remainingAttempts = new AtomicInteger();
        AtomicInteger shouldBlockCurrentRequest = new AtomicInteger(0);

        attempts.compute(ip, (key, existingEntry) -> {
            if (existingEntry == null || existingEntry.getFirstAttempt().isBefore(windowStart)) {
                int newCount = 1;
                attemptCount.set(newCount);
                remainingAttempts.set(Math.max(0, properties.getMaxAttempts() - newCount));
                shouldBlockCurrentRequest.set(0);
                return new RateLimitEntry(now);
            } else {
                int previousCount = existingEntry.getAttemptCount().get();
                int newCount = existingEntry.getAttemptCount().incrementAndGet();
                attemptCount.set(newCount);
                remainingAttempts.set(Math.max(0, properties.getMaxAttempts() - newCount));
                shouldBlockCurrentRequest.set(newCount > properties.getMaxAttempts() ? 1 : 0);

                if (newCount >= properties.getMaxAttempts()) {
                    if (previousCount < properties.getMaxAttempts()) {
                        logger.warn("IP {} has been blocked due to too many login attempts ({} attempts)", ip, newCount);
                    }
                    existingEntry.block();
                }
                return existingEntry;
            }
        });

        RateLimitEntry entry = attempts.get(ip);
        long retryAfterSeconds = entry == null ? 0 : getSecondsUntilReset(entry, now);

        return new FailedAttemptResult(
                shouldBlockCurrentRequest.get() == 1,
                remainingAttempts.get(),
                retryAfterSeconds
        );
    }

    public void recordAttempt(String ip) {
        recordFailedAttempt(ip);
    }
    
    public int getRemainingAttempts(String ip) {
        if (!isRateLimitingActive() || (isLocalhost(ip) && properties.isWhitelistLocalhost())) {
            return Integer.MAX_VALUE;
        }

        Instant now = Instant.now();
        RateLimitEntry entry = getActiveEntry(ip, now);
        if (entry == null) {
            return properties.getMaxAttempts();
        }

        if (entry.isBlocked()) {
            return 0;
        }

        int remaining = properties.getMaxAttempts() - entry.getAttemptCount().get();
        return Math.max(0, remaining);
    }
    
    public long getSecondsUntilReset(String ip) {
        if (!isRateLimitingActive() || (isLocalhost(ip) && properties.isWhitelistLocalhost())) {
            return 0;
        }
        
        Instant now = Instant.now();
        RateLimitEntry entry = getActiveEntry(ip, now);
        if (entry == null) {
            return 0;
        }

        return getSecondsUntilReset(entry, now);
    }

    private RateLimitEntry getActiveEntry(String ip, Instant now) {
        RateLimitEntry entry = attempts.get(ip);
        if (entry == null) {
            return null;
        }

        Instant windowStart = now.minusSeconds(properties.getWindowMinutes() * 60L);
        if (entry.getFirstAttempt().isBefore(windowStart)) {
            attempts.computeIfPresent(ip, (key, existingEntry) ->
                    existingEntry.getFirstAttempt().isBefore(windowStart) ? null : existingEntry
            );
            return null;
        }

        return entry;
    }

    private long getSecondsUntilReset(RateLimitEntry entry, Instant now) {
        Instant windowEnd = entry.getFirstAttempt().plusSeconds(properties.getWindowMinutes() * 60L);

        if (windowEnd.isAfter(now)) {
            return windowEnd.getEpochSecond() - now.getEpochSecond();
        }

        return 0;
    }
    
    private void cleanup() {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(properties.getWindowMinutes() * 60L);
        
        int removedCount = attempts.size();
        attempts.entrySet().removeIf(entry -> entry.getValue().getFirstAttempt().isBefore(windowStart));
        removedCount -= attempts.size();
        
        if (removedCount > 0) {
            logger.debug("Cleaned up {} expired rate limit entries", removedCount);
        }
    }

    private boolean isRateLimitingActive() {
        return properties.isEnabled() && authProperties.isAuthenticationEnabled();
    }
    
    private boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) || 
               "0:0:0:0:0:0:0:1".equals(ip) || 
               "::1".equals(ip) ||
               "localhost".equalsIgnoreCase(ip);
    }
    
    static class RateLimitEntry {
        private final Instant firstAttempt;
        private final AtomicInteger attemptCount;
        private volatile boolean blocked;
        
        RateLimitEntry(Instant firstAttempt) {
            this.firstAttempt = firstAttempt;
            this.attemptCount = new AtomicInteger(1);
            this.blocked = false;
        }
        
        Instant getFirstAttempt() {
            return firstAttempt;
        }
        
        AtomicInteger getAttemptCount() {
            return attemptCount;
        }
        
        boolean isBlocked() {
            return blocked;
        }
        
        void block() {
            this.blocked = true;
        }
    }

    static class FailedAttemptResult {
        private final boolean shouldBlockCurrentRequest;
        private final int remainingAttempts;
        private final long retryAfterSeconds;

        FailedAttemptResult(boolean shouldBlockCurrentRequest, int remainingAttempts, long retryAfterSeconds) {
            this.shouldBlockCurrentRequest = shouldBlockCurrentRequest;
            this.remainingAttempts = remainingAttempts;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        boolean shouldBlockCurrentRequest() {
            return shouldBlockCurrentRequest;
        }

        int remainingAttempts() {
            return remainingAttempts;
        }

        long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
