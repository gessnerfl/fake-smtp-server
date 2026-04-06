package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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
    void shouldOnlyBlockCurrentRequestAfterMaxAttemptsAreExceeded() {
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(createProperties(3), createAuthProperties(true));
        String ip = "198.51.100.21";

        assertFalse(rateLimiter.recordFailedAttempt(ip).shouldBlockCurrentRequest());
        assertFalse(rateLimiter.recordFailedAttempt(ip).shouldBlockCurrentRequest());
        assertFalse(rateLimiter.recordFailedAttempt(ip).shouldBlockCurrentRequest());
        assertTrue(rateLimiter.recordFailedAttempt(ip).shouldBlockCurrentRequest());
    }

    @Test
    void shouldBlockParallelRequestsThatExceedLimit() throws Exception {
        int maxAttempts = 3;
        int requestCount = 10;
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(createProperties(maxAttempts), createAuthProperties(true));
        String ip = "198.51.100.22";

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return rateLimiter.recordFailedAttempt(ip).shouldBlockCurrentRequest();
                }));
            }

            startLatch.countDown();

            int blockedCurrentRequestCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    blockedCurrentRequestCount++;
                }
            }

            assertEquals(requestCount - maxAttempts, blockedCurrentRequestCount);
            assertFalse(rateLimiter.isAllowed(ip));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldReturnMaxRemainingAttemptsForExpiredWindow() throws Exception {
        int maxAttempts = 3;
        InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(createProperties(maxAttempts), createAuthProperties(true));
        String ip = "198.51.100.23";

        Map<String, InMemoryRateLimiter.RateLimitEntry> attempts = attemptsMap(rateLimiter);
        attempts.put(ip, new InMemoryRateLimiter.RateLimitEntry(Instant.now().minusSeconds(61)));

        assertEquals(maxAttempts, rateLimiter.getRemainingAttempts(ip));
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
}
