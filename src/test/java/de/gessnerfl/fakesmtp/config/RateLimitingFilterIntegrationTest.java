package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("mockserver")
@SpringBootTest(properties = {
        "fakesmtp.webapp.authentication.enabled=true",
        "fakesmtp.webapp.authentication.username=testuser",
        "fakesmtp.webapp.authentication.password=testpass",
        "fakesmtp.webapp.rate-limiting.enabled=true",
        "fakesmtp.webapp.rate-limiting.max-attempts=3",
        "fakesmtp.webapp.rate-limiting.window-minutes=1",
        "fakesmtp.webapp.rate-limiting.trust-proxy-headers=true"
})
@AutoConfigureMockMvc
class RateLimitingFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowLoginAttemptsWithinLimit() throws Exception {
        String clientIp = "192.168.1.1";
        
        // First 3 attempts should be allowed (max-attempts=3)
        // Note: Header shows remaining BEFORE current attempt is recorded
        for (int i = 0; i < 3; i++) {
            // Get fresh CSRF token for each attempt
            MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                    .andExpect(status().isOk())
                    .andReturn();
            
            String csrfToken = extractCsrfTokenFromCookies(metaResult);
            jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
            MockHttpSession session = new MockHttpSession();
            
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                            .session(session)
                            .header("X-Forwarded-For", clientIp)
                            .param("username", "testuser")
                            .param("password", "wrongpass")
                            .cookie(csrfCookie)
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists("X-RateLimit-Remaining"))
                    .andReturn();
            
            String remainingHeader = result.getResponse().getHeader("X-RateLimit-Remaining");
            int expectedRemaining = 3 - i; // 3, 2, 1 (before each attempt is recorded)
            assertEquals(String.valueOf(expectedRemaining), remainingHeader, 
                    "Expected " + expectedRemaining + " remaining attempts before attempt " + (i + 1));
        }
    }

    @Test
    void shouldBlockLoginAttemptsAfterLimitExceeded() throws Exception {
        String clientIp = "192.168.1.2";
        
        // Use up all allowed attempts (max-attempts=3)
        for (int i = 0; i < 3; i++) {
            MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                    .andExpect(status().isOk())
                    .andReturn();
            
            String csrfToken = extractCsrfTokenFromCookies(metaResult);
            jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
            MockHttpSession session = new MockHttpSession();
            
            mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                            .session(session)
                            .header("X-Forwarded-For", clientIp)
                            .param("username", "testuser")
                            .param("password", "wrongpass")
                            .cookie(csrfCookie)
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isUnauthorized());
        }
        
        // 4th attempt should be blocked by rate limiter (no CSRF needed as it blocks before security)
        MvcResult blockedResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .param("username", "testuser")
                        .param("password", "wrongpass"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.error").value("Too many requests. Please try again later."))
                .andReturn();

        long retryAfter = Long.parseLong(blockedResult.getResponse().getHeader("Retry-After"));
        assertTrue(retryAfter >= 1 && retryAfter <= 60,
                "Expected Retry-After between 1 and 60 seconds but was " + retryAfter);
    }

    @Test
    void shouldNotCountSuccessfulLoginsAsRateLimitAttempts() throws Exception {
        String clientIp = "192.168.1.50";

        MvcResult successfulLoginMeta = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();

        String successfulLoginCsrfToken = extractCsrfTokenFromCookies(successfulLoginMeta);
        jakarta.servlet.http.Cookie successfulLoginCsrfCookie = extractCsrfCookie(successfulLoginMeta);
        MockHttpSession successfulLoginSession = new MockHttpSession();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(successfulLoginSession)
                        .header("X-Forwarded-For", clientIp)
                        .param("username", "testuser")
                        .param("password", "testpass")
                        .cookie(successfulLoginCsrfCookie)
                        .header("X-XSRF-TOKEN", successfulLoginCsrfToken))
                .andExpect(status().isNoContent())
                .andExpect(header().string("X-RateLimit-Remaining", "3"));

        MvcResult failedLoginMeta = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();

        String failedLoginCsrfToken = extractCsrfTokenFromCookies(failedLoginMeta);
        jakarta.servlet.http.Cookie failedLoginCsrfCookie = extractCsrfCookie(failedLoginMeta);
        MockHttpSession failedLoginSession = new MockHttpSession();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(failedLoginSession)
                        .header("X-Forwarded-For", clientIp)
                        .param("username", "testuser")
                        .param("password", "wrongpass")
                        .cookie(failedLoginCsrfCookie)
                        .header("X-XSRF-TOKEN", failedLoginCsrfToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "3"));
    }

    @Test
    void shouldWhitelistLocalhost() throws Exception {
        String[] localhostIps = {"127.0.0.1", "::1", "0:0:0:0:0:0:0:1"};
        
        for (String localhostIp : localhostIps) {
            // Make many attempts from localhost - should never be blocked
            for (int i = 0; i < 10; i++) {
                MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                        .andExpect(status().isOk())
                        .andReturn();
                
                String csrfToken = extractCsrfTokenFromCookies(metaResult);
                jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
                MockHttpSession session = new MockHttpSession();
                
                mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                                .session(session)
                                .header("X-Forwarded-For", localhostIp)
                                .param("username", "testuser")
                                .param("password", "wrongpass")
                                .cookie(csrfCookie)
                                .header("X-XSRF-TOKEN", csrfToken))
                        .andExpect(status().isUnauthorized())
                        .andExpect(header().doesNotExist("X-RateLimit-Remaining"));
            }
        }
    }

    @Test
    void shouldNotApplyRateLimitingToNonLoginEndpoints() throws Exception {
        String clientIp = "192.168.1.3";
        
        // Make multiple requests to non-login endpoints
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/meta-data")
                            .header("X-Forwarded-For", clientIp))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("X-RateLimit-Remaining"));
        }
    }

    @Test
    void shouldApplyRateLimitingToLoginRequestsWithContextPath() throws Exception {
        String clientIp = "192.168.1.60";
        String contextPath = "/fakesmtp";

        for (int i = 0; i < 3; i++) {
            MvcResult metaResult = mockMvc.perform(get(contextPath + "/api/meta-data")
                            .contextPath(contextPath)
                            .servletPath("/api/meta-data"))
                    .andExpect(status().isOk())
                    .andReturn();

            String csrfToken = extractCsrfTokenFromCookies(metaResult);
            jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
            MockHttpSession session = new MockHttpSession();

            mockMvc.perform(MockMvcRequestBuilders.post(contextPath + "/api/auth/login")
                            .contextPath(contextPath)
                            .servletPath("/api/auth/login")
                            .session(session)
                            .header("X-Forwarded-For", clientIp)
                            .param("username", "testuser")
                            .param("password", "wrongpass")
                            .cookie(csrfCookie)
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(MockMvcRequestBuilders.post(contextPath + "/api/auth/login")
                        .contextPath(contextPath)
                        .servletPath("/api/auth/login")
                        .header("X-Forwarded-For", clientIp)
                        .param("username", "testuser")
                        .param("password", "wrongpass"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }

    @Test
    void shouldExtractClientIpFromXForwardedFor() throws Exception {
        String clientIp = "10.0.0.1";
        
        // First request sets up tracking
        MvcResult metaResult1 = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        
        String csrfToken1 = extractCsrfTokenFromCookies(metaResult1);
        jakarta.servlet.http.Cookie csrfCookie1 = extractCsrfCookie(metaResult1);
        MockHttpSession session1 = new MockHttpSession();
        
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(session1)
                        .header("X-Forwarded-For", clientIp)
                        .param("username", "testuser")
                        .param("password", "wrongpass")
                        .cookie(csrfCookie1)
                        .header("X-XSRF-TOKEN", csrfToken1))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "3")); // Before first attempt
    }

    @Test
    void shouldExtractFirstIpFromXForwardedForChain() throws Exception {
        // When multiple IPs are in X-Forwarded-For, the first one (client) should be used
        String clientIp = "10.0.0.2";
        String proxyChain = clientIp + ", 192.168.1.100, 172.16.0.1";
        
        MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        
        String csrfToken = extractCsrfTokenFromCookies(metaResult);
        jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
        MockHttpSession session = new MockHttpSession();
        
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(session)
                        .header("X-Forwarded-For", proxyChain)
                        .param("username", "testuser")
                        .param("password", "wrongpass")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "3")); // Before first attempt
    }

    @Test
    void shouldUseXRealIpWhenXForwardedForNotPresent() throws Exception {
        String clientIp = "10.0.0.3";
        
        MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        
        String csrfToken = extractCsrfTokenFromCookies(metaResult);
        jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
        MockHttpSession session = new MockHttpSession();
        
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(session)
                        .header("X-Real-IP", clientIp)
                        .param("username", "testuser")
                        .param("password", "wrongpass")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "3")); // Before first attempt
    }

    @Test
    void shouldTrackDifferentClientsIndependently() throws Exception {
        String clientIp1 = "192.168.1.10";
        String clientIp2 = "192.168.1.11";
        
        // Exhaust limit for client 1
        for (int i = 0; i < 3; i++) {
            MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                    .andExpect(status().isOk())
                    .andReturn();
            
            String csrfToken = extractCsrfTokenFromCookies(metaResult);
            jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
            MockHttpSession session = new MockHttpSession();
            
            mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                            .session(session)
                            .header("X-Forwarded-For", clientIp1)
                            .param("username", "testuser")
                            .param("password", "wrongpass")
                            .cookie(csrfCookie)
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isUnauthorized());
        }
        
        // Client 1 should now be blocked (no CSRF needed as rate limiter blocks first)
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .header("X-Forwarded-For", clientIp1)
                        .param("username", "testuser")
                        .param("password", "wrongpass"))
                .andExpect(status().isTooManyRequests());
        
        // Client 2 should still be allowed
        MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        
        String csrfToken = extractCsrfTokenFromCookies(metaResult);
        jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
        MockHttpSession session = new MockHttpSession();
        
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(session)
                        .header("X-Forwarded-For", clientIp2)
                        .param("username", "testuser")
                        .param("password", "wrongpass")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "3")); // Before first attempt for client 2
    }

    @Test
    void shouldUseLeftmostForwardedIpForRateLimitBuckets() throws Exception {
        String firstProxyChain = "10.0.0.42, 198.51.100.7";
        String secondProxyChain = "10.0.0.42, 198.51.100.8";
        String thirdProxyChain = "10.0.0.43, 198.51.100.7";

        performFailedLogin(firstProxyChain)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "3"));

        performFailedLogin(secondProxyChain)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "2"));

        performFailedLogin(thirdProxyChain)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "3"));
    }

    @Test
    void shouldUseFirstNonEmptyForwardedValueWhenHeaderContainsEmptyEntries() throws Exception {
        performFailedLogin(" ,  , 10.0.0.52")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "3"));

        performFailedLogin("10.0.0.52, 198.51.100.9")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "2"));
    }

    private org.springframework.test.web.servlet.ResultActions performFailedLogin(String forwardedForHeader) throws Exception {
        MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();

        String csrfToken = extractCsrfTokenFromCookies(metaResult);
        jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
        MockHttpSession session = new MockHttpSession();

        return mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .session(session)
                .header("X-Forwarded-For", forwardedForHeader)
                .param("username", "testuser")
                .param("password", "wrongpass")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfToken));
    }
    
    private String extractCsrfTokenFromCookies(MvcResult result) {
        jakarta.servlet.http.Cookie cookie = extractCsrfCookie(result);
        return cookie != null ? cookie.getValue() : "";
    }
    
    private jakarta.servlet.http.Cookie extractCsrfCookie(MvcResult result) {
        if (result.getResponse().getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : result.getResponse().getCookies()) {
                if ("XSRF-TOKEN".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                    jakarta.servlet.http.Cookie copy = new jakarta.servlet.http.Cookie(cookie.getName(), cookie.getValue());
                    copy.setPath(cookie.getPath() != null ? cookie.getPath() : "/");
                    copy.setMaxAge(cookie.getMaxAge());
                    copy.setSecure(cookie.getSecure());
                    return copy;
                }
            }
        }
        return null;
    }
}
