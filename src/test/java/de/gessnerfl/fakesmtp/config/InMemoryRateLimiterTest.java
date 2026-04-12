package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRateLimiterTest {

    @Test
    void shouldReserveAndReleaseSuccessfulAttemptsWithoutConsumingTheLimit() throws Exception {
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(createProperties(3), createAuthProperties(true));
        String ip = "198.51.100.21";

        InMemoryRateLimiter.AttemptReservation reservation = rateLimiter.reserveLoginAttempt(ip);

        assertFalse(reservation.blocked());
        assertEquals(3, reservation.remainingAttempts());

        rateLimiter.releaseAttempt(reservation);

        assertEquals(3, rateLimiter.getRemainingAttempts(ip));
        assertTrue(attemptsMap(rateLimiter).isEmpty());
    }

    @Test
    void shouldCommitFailedAttemptsOnlyAfterAuthenticationFails() {
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(createProperties(3), createAuthProperties(true));
        String ip = "198.51.100.22";

        InMemoryRateLimiter.AttemptReservation firstReservation = rateLimiter.reserveLoginAttempt(ip);
        assertFalse(firstReservation.blocked());
        assertEquals(3, firstReservation.remainingAttempts());

        rateLimiter.commitFailedAttempt(firstReservation);

        assertEquals(2, rateLimiter.getRemainingAttempts(ip));
        assertFalse(rateLimiter.reserveLoginAttempt(ip).blocked());
    }

    @Test
    void shouldBlockParallelReservationsThatExceedLimit() throws Exception {
        int maxAttempts = 3;
        int requestCount = 10;
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(createProperties(maxAttempts), createAuthProperties(true));
        String ip = "198.51.100.23";

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Future<InMemoryRateLimiter.AttemptReservation>> futures = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return rateLimiter.reserveLoginAttempt(ip);
                }));
            }

            startLatch.countDown();

            int blockedCurrentRequestCount = 0;
            List<InMemoryRateLimiter.AttemptReservation> allowedReservations = new ArrayList<>();
            for (Future<InMemoryRateLimiter.AttemptReservation> future : futures) {
                InMemoryRateLimiter.AttemptReservation reservation = future.get();
                if (reservation.blocked()) {
                    blockedCurrentRequestCount++;
                } else {
                    allowedReservations.add(reservation);
                }
            }

            assertEquals(requestCount - maxAttempts, blockedCurrentRequestCount);
            allowedReservations.forEach(rateLimiter::releaseAttempt);
            assertEquals(maxAttempts, rateLimiter.getRemainingAttempts(ip));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldReturnMaxRemainingAttemptsForExpiredWindow() throws Exception {
        int maxAttempts = 3;
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(createProperties(maxAttempts), createAuthProperties(true));
        String ip = "198.51.100.24";

        Map<String, InMemoryRateLimiter.RateLimitEntry> attempts = attemptsMap(rateLimiter);
        attempts.put(ip, new InMemoryRateLimiter.RateLimitEntry(Instant.now().minusSeconds(61)));

        assertEquals(maxAttempts, rateLimiter.getRemainingAttempts(ip));
    }

    @Test
    void shouldKeepReservedEntriesDuringCleanupAndStartWindowWhenTheFailureFinallyCommits() throws Exception {
        int maxAttempts = 3;
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(createProperties(maxAttempts), createAuthProperties(true));
        String ip = "198.51.100.25";

        InMemoryRateLimiter.RateLimitEntry expiredReservedEntry = new InMemoryRateLimiter.RateLimitEntry(Instant.now().minusSeconds(61));
        expiredReservedEntry.getReservedAttempts().incrementAndGet();
        attemptsMap(rateLimiter).put(ip, expiredReservedEntry);

        invokeCleanup(rateLimiter);

        rateLimiter.commitFailedAttempt(new InMemoryRateLimiter.AttemptReservation(ip, expiredReservedEntry, false, maxAttempts, 0));

        assertEquals(2, rateLimiter.getRemainingAttempts(ip));
        assertTrue(rateLimiter.getSecondsUntilReset(ip) > 0);
        assertTrue(attemptsMap(rateLimiter).containsKey(ip));
    }

    private static RateLimitingProperties createProperties(int maxAttempts) {
        RateLimitingProperties properties = new RateLimitingProperties();
        properties.setEnabled(true);
        properties.setMaxAttempts(maxAttempts);
        properties.setWindowMinutes(1);
        properties.setWhitelistLocalhost(false);
        return properties;
    }

    private static WebappAuthenticationProperties createAuthProperties(boolean enabled) {
        WebappAuthenticationProperties properties = new WebappAuthenticationProperties();
        properties.setEnabled(enabled);
        properties.setUsername("testuser");
        properties.setPassword("testpass");
        return properties;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, InMemoryRateLimiter.RateLimitEntry> attemptsMap(InMemoryRateLimiter rateLimiter) throws Exception {
        Field attemptsField = InMemoryRateLimiter.class.getDeclaredField("attempts");
        attemptsField.setAccessible(true);
        return (Map<String, InMemoryRateLimiter.RateLimitEntry>) attemptsField.get(rateLimiter);
    }

    private static void invokeCleanup(InMemoryRateLimiter rateLimiter) throws Exception {
        Method cleanupMethod = InMemoryRateLimiter.class.getDeclaredMethod("cleanup");
        cleanupMethod.setAccessible(true);
        cleanupMethod.invoke(rateLimiter);
    }
}
