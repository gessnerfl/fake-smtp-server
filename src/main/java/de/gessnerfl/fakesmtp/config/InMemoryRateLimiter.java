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
    private final Map<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleanupExecutor;
    
    public InMemoryRateLimiter(RateLimitingProperties properties) {
        this.properties = properties;
    }
    
    @PostConstruct
    public void init() {
        if (properties.isEnabled()) {
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
        if (!properties.isEnabled()) {
            return true;
        }
        
        if (isLocalhost(ip) && properties.isWhitelistLocalhost()) {
            return true;
        }
        
        RateLimitEntry entry = attempts.get(ip);
        if (entry == null) {
            return true;
        }
        
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(properties.getWindowMinutes() * 60L);
        
        if (entry.getFirstAttempt().isBefore(windowStart)) {
            return true;
        }
        
        if (entry.isBlocked()) {
            return false;
        }
        
        return entry.getAttemptCount().get() < properties.getMaxAttempts();
    }
    
    public void recordAttempt(String ip) {
        if (!properties.isEnabled() || (isLocalhost(ip) && properties.isWhitelistLocalhost())) {
            return;
        }
        
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(properties.getWindowMinutes() * 60L);
        
        attempts.compute(ip, (key, existingEntry) -> {
            if (existingEntry == null || existingEntry.getFirstAttempt().isBefore(windowStart)) {
                return new RateLimitEntry(now);
            } else {
                int newCount = existingEntry.getAttemptCount().incrementAndGet();
                if (newCount >= properties.getMaxAttempts()) {
                    existingEntry.block();
                    logger.warn("IP {} has been blocked due to too many login attempts ({} attempts)", ip, newCount);
                }
                return existingEntry;
            }
        });
    }
    
    public int getRemainingAttempts(String ip) {
        if (!properties.isEnabled() || (isLocalhost(ip) && properties.isWhitelistLocalhost())) {
            return Integer.MAX_VALUE;
        }
        
        RateLimitEntry entry = attempts.get(ip);
        if (entry == null) {
            return properties.getMaxAttempts();
        }
        
        if (entry.isBlocked()) {
            return 0;
        }
        
        int remaining = properties.getMaxAttempts() - entry.getAttemptCount().get();
        return Math.max(0, remaining);
    }
    
    private void cleanup() {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(properties.getWindowMinutes() * 60L);
        
        int removedCount = 0;
        for (Map.Entry<String, RateLimitEntry> entry : attempts.entrySet()) {
            if (entry.getValue().getFirstAttempt().isBefore(windowStart)) {
                attempts.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.debug("Cleaned up {} expired rate limit entries", removedCount);
        }
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
}
