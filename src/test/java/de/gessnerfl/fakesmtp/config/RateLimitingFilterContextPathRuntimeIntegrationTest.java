package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("mockserver")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.servlet.context-path=/fakesmtp",
                "fakesmtp.webapp.authentication.enabled=true",
                "fakesmtp.webapp.authentication.username=testuser",
                "fakesmtp.webapp.authentication.password=testpass",
                "fakesmtp.webapp.rate-limiting.enabled=true",
                "fakesmtp.webapp.rate-limiting.max-attempts=3",
                "fakesmtp.webapp.rate-limiting.window-minutes=1",
                "fakesmtp.webapp.rate-limiting.trust-proxy-headers=true"
        }
)
class RateLimitingFilterContextPathRuntimeIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void shouldRateLimitLoginRequestsWhenServletContextPathIsConfigured() {
        String clientIp = "192.168.1.70";

        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> loginResponse = performFailedLogin(clientIp);
            assertEquals(401, loginResponse.getStatusCode().value());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", clientIp);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", "testuser");
        body.add("password", "wrongpass");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> blockedResponse = postForEntityAllowingError(
                baseUrl() + "/api/auth/login",
                request,
                String.class
        );

        assertEquals(429, blockedResponse.getStatusCode().value());
        assertEquals("0", blockedResponse.getHeaders().getFirst("X-RateLimit-Remaining"));
        long retryAfter = Long.parseLong(blockedResponse.getHeaders().getFirst("Retry-After"));
        assertTrue(retryAfter >= 1 && retryAfter <= 60,
                "Expected Retry-After between 1 and 60 seconds but was " + retryAfter);
    }

    @Test
    void shouldAllowSuccessfulLoginWhenServletContextPathIsConfigured() {
        ResponseEntity<String> loginResponse = performLogin("192.168.1.71", "testpass");
        assertEquals(204, loginResponse.getStatusCode().value());
        assertEquals("3", loginResponse.getHeaders().getFirst("X-RateLimit-Remaining"));
    }

    private ResponseEntity<String> performFailedLogin(String clientIp) {
        return performLogin(clientIp, "wrongpass");
    }

    private ResponseEntity<String> performLogin(String clientIp, String password) {
        ResponseEntity<String> metaDataResponse = restTemplate.getForEntity(baseUrl() + "/api/meta-data", String.class);
        assertEquals(200, metaDataResponse.getStatusCode().value());

        List<String> setCookieHeaders = metaDataResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        String csrfToken = extractCookieValue(setCookieHeaders, "XSRF-TOKEN");
        assertNotNull(csrfToken, "XSRF-TOKEN cookie is required for login requests");

        String jsessionId = extractCookieValue(setCookieHeaders, "JSESSIONID");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("X-Forwarded-For", clientIp);
        headers.add("X-XSRF-TOKEN", csrfToken);
        headers.add(HttpHeaders.COOKIE, buildCookieHeader(csrfToken, jsessionId));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "testuser");
        formData.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        return postForEntityAllowingError(baseUrl() + "/api/auth/login", request, String.class);
    }

    private static String buildCookieHeader(String csrfToken, String jsessionId) {
        StringBuilder cookieHeader = new StringBuilder("XSRF-TOKEN=").append(csrfToken);
        if (jsessionId != null && !jsessionId.isEmpty()) {
            cookieHeader.append("; JSESSIONID=").append(jsessionId);
        }
        return cookieHeader.toString();
    }

    private static String extractCookieValue(List<String> setCookieHeaders, String cookieName) {
        if (setCookieHeaders == null) {
            return null;
        }

        String prefix = cookieName + "=";
        for (String header : setCookieHeaders) {
            if (header == null || header.isEmpty()) {
                continue;
            }
            String[] segments = header.split(";");
            if (segments.length == 0) {
                continue;
            }

            String cookiePart = segments[0].trim();
            if (cookiePart.startsWith(prefix)) {
                return cookiePart.substring(prefix.length());
            }
        }

        return null;
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/fakesmtp";
    }

    private <T> ResponseEntity<T> postForEntityAllowingError(String url, HttpEntity<?> request, Class<T> responseType) {
        try {
            return restTemplate.postForEntity(url, request, responseType);
        } catch (HttpStatusCodeException ex) {
            if (responseType == String.class) {
                @SuppressWarnings("unchecked")
                T body = (T) ex.getResponseBodyAsString();
                return new ResponseEntity<>(body, ex.getResponseHeaders(), ex.getStatusCode());
            }
            return new ResponseEntity<>(ex.getResponseHeaders(), ex.getStatusCode());
        }
    }
}
