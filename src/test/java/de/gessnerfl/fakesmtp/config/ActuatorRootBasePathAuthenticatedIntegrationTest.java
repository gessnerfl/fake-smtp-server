package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("mockserver")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0",
                "management.endpoints.web.base-path=/",
                "management.endpoints.web.exposure.include=health,info,metrics",
                "fakesmtp.webapp.authentication.enabled=true",
                "fakesmtp.webapp.authentication.username=testuser",
                "fakesmtp.webapp.authentication.password=testpass"
        }
)
class ActuatorRootBasePathAuthenticatedIntegrationTest {

    @Value("${local.management.port}")
    private int managementPort;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void shouldAllowHttpBasicAuthenticatedAccessToMetricsWhenBasePathIsRoot() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("testuser", "testpass");

        assertEquals(200, exchangeAllowingError(managementUrl("/metrics"), HttpMethod.GET, new HttpEntity<>(headers)).getStatusCode().value());
    }

    private ResponseEntity<String> exchangeAllowingError(String path, HttpMethod method, HttpEntity<?> request) {
        try {
            return restTemplate.exchange(path, method, request, String.class);
        } catch (HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode());
        }
    }

    private String managementUrl(String path) {
        return "http://localhost:" + managementPort + path;
    }
}
