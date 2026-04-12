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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
        return getRemainingAttempts(ip) > 0;
    }

    public AttemptReservation reserveLoginAttempt(String ip) {
        if (!isRateLimitingActive() || (isLocalhost(ip) && properties.isWhitelistLocalhost())) {
            return new AttemptReservation(ip, null, false, Integer.MAX_VALUE, 0);
        }

        Instant now = Instant.now();
        AtomicReference<RateLimitEntry> entryRef = new AtomicReference<>();
        AtomicInteger remainingAttempts = new AtomicInteger();
        AtomicLong retryAfterSeconds = new AtomicLong();
        AtomicBoolean blocked = new AtomicBoolean();

        attempts.compute(ip, (key, existingEntry) -> {
            RateLimitEntry activeEntry = existingEntry;
            if (activeEntry == null) {
                activeEntry = new RateLimitEntry();
            }
            normalizeEntry(activeEntry, now);

            entryRef.set(activeEntry);

            if (activeEntry.isBlocked()) {
                blocked.set(true);
                remainingAttempts.set(0);
                retryAfterSeconds.set(getSecondsUntilReset(activeEntry, now));
                return activeEntry;
            }

            int availableAttempts = activeEntry.getAvailableAttempts(properties.getMaxAttempts());
            if (availableAttempts <= 0) {
                blocked.set(true);
                remainingAttempts.set(0);
                retryAfterSeconds.set(getSecondsUntilReset(activeEntry, now));
                return activeEntry;
            }

            activeEntry.getReservedAttempts().incrementAndGet();
            remainingAttempts.set(availableAttempts);
            retryAfterSeconds.set(0);
            return activeEntry;
        });

        return new AttemptReservation(
                ip,
                entryRef.get(),
                blocked.get(),
                remainingAttempts.get(),
                retryAfterSeconds.get()
        );
    }

    public void commitFailedAttempt(AttemptReservation reservation) {
        if (reservation == null || reservation.blocked() || reservation.entry() == null) {
            return;
        }

        if (!isRateLimitingActive() || (isLocalhost(reservation.ip()) && properties.isWhitelistLocalhost())) {
            return;
        }

        Instant now = Instant.now();
        attempts.computeIfPresent(reservation.ip(), (ip, entry) -> {
            if (entry != reservation.entry()) {
                return entry;
            }

            entry.decrementReservedAttempts();
            normalizeEntry(entry, now);

            if (entry.getFailedAttempts().get() == 0) {
                entry.setFirstFailedAttempt(now);
            }

            int failedAttempts = entry.getFailedAttempts().incrementAndGet();
            if (failedAttempts >= properties.getMaxAttempts()) {
                if (!entry.isBlocked()) {
                    logger.warn("IP {} has been blocked due to too many login attempts ({} attempts)", reservation.ip(), failedAttempts);
                }
                entry.block();
            }
            return entry;
        });
    }

    public void releaseAttempt(AttemptReservation reservation) {
        if (reservation == null || reservation.blocked() || reservation.entry() == null) {
            return;
        }

        if (!isRateLimitingActive() || (isLocalhost(reservation.ip()) && properties.isWhitelistLocalhost())) {
            return;
        }

        Instant now = Instant.now();
        attempts.computeIfPresent(reservation.ip(), (ip, entry) -> {
            if (entry != reservation.entry()) {
                return entry;
            }

            entry.decrementReservedAttempts();
            normalizeEntry(entry, now);
            return entry.isIdle() ? null : entry;
        });
    }

    public FailedAttemptResult recordFailedAttempt(String ip) {
        AttemptReservation reservation = reserveLoginAttempt(ip);
        if (reservation.blocked()) {
            return new FailedAttemptResult(true, 0, reservation.retryAfterSeconds());
        }

        commitFailedAttempt(reservation);
        return new FailedAttemptResult(false, getRemainingAttempts(ip), reservation.retryAfterSeconds());
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

        int remaining = properties.getMaxAttempts() - entry.getFailedAttempts().get() - entry.getReservedAttempts().get();
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
        AtomicReference<RateLimitEntry> entryRef = new AtomicReference<>();
        attempts.computeIfPresent(ip, (key, entry) -> {
            normalizeEntry(entry, now);
            if (entry.isIdle()) {
                entryRef.set(null);
                return null;
            }

            entryRef.set(entry);
            return entry;
        });
        return entryRef.get();
    }

    private long getSecondsUntilReset(RateLimitEntry entry, Instant now) {
        Instant firstFailedAttempt = entry.getFirstFailedAttempt();
        if (firstFailedAttempt == null) {
            return 0;
        }

        Instant windowEnd = firstFailedAttempt.plusSeconds(properties.getWindowMinutes() * 60L);

        if (windowEnd.isAfter(now)) {
            return windowEnd.getEpochSecond() - now.getEpochSecond();
        }

        return 0;
    }
    
    private void cleanup() {
        Instant now = Instant.now();
        int removedCount = attempts.size();
        attempts.forEach((ip, ignored) -> attempts.computeIfPresent(ip, (key, entry) -> {
            normalizeEntry(entry, now);
            return entry.isIdle() ? null : entry;
        }));
        removedCount -= attempts.size();
        
        if (removedCount > 0) {
            logger.debug("Cleaned up {} expired rate limit entries", removedCount);
        }
    }

    private void normalizeEntry(RateLimitEntry entry, Instant now) {
        Instant firstFailedAttempt = entry.getFirstFailedAttempt();
        if (firstFailedAttempt == null) {
            return;
        }

        Instant windowStart = now.minusSeconds(properties.getWindowMinutes() * 60L);
        if (firstFailedAttempt.isBefore(windowStart)) {
            entry.clearFailedAttempts();
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
        private final AtomicReference<Instant> firstFailedAttempt;
        private final AtomicInteger failedAttempts;
        private final AtomicInteger reservedAttempts;
        private volatile boolean blocked;

        RateLimitEntry() {
            this(null);
        }

        RateLimitEntry(Instant firstAttempt) {
            this.firstFailedAttempt = new AtomicReference<>(firstAttempt);
            this.failedAttempts = new AtomicInteger(0);
            this.reservedAttempts = new AtomicInteger(0);
            this.blocked = false;
        }

        Instant getFirstFailedAttempt() {
            return firstFailedAttempt.get();
        }

        void setFirstFailedAttempt(Instant firstFailedAttempt) {
            this.firstFailedAttempt.set(firstFailedAttempt);
        }

        AtomicInteger getFailedAttempts() {
            return failedAttempts;
        }

        AtomicInteger getReservedAttempts() {
            return reservedAttempts;
        }
        
        boolean isBlocked() {
            return blocked;
        }
        
        void block() {
            this.blocked = true;
        }

        void clearFailedAttempts() {
            failedAttempts.set(0);
            blocked = false;
            firstFailedAttempt.set(null);
        }

        void decrementReservedAttempts() {
            reservedAttempts.updateAndGet(value -> Math.max(0, value - 1));
        }

        int getAvailableAttempts(int maxAttempts) {
            return Math.max(0, maxAttempts - failedAttempts.get() - reservedAttempts.get());
        }

        boolean isIdle() {
            return failedAttempts.get() == 0 && reservedAttempts.get() == 0;
        }
    }

    record AttemptReservation(String ip, RateLimitEntry entry, boolean blocked, int remainingAttempts, long retryAfterSeconds) {}

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
