package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
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
                "fakesmtp.webapp.authentication.username=testuser",
                "fakesmtp.webapp.authentication.password=testpass"
        }
)
class ActuatorRootBasePathSecurityIntegrationTest {

    @Value("${local.server.port}")
    private int applicationPort;

    @Value("${local.management.port}")
    private int managementPort;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void shouldOnlyProtectNonPublicActuatorEndpointsWhenBasePathIsRoot() {
        assertEquals(200, restTemplate.getForEntity(managementUrl("/health"), String.class).getStatusCode().value());
        assertEquals(200, restTemplate.getForEntity(managementUrl("/info"), String.class).getStatusCode().value());
        assertEquals(401, getForEntityAllowingError(managementUrl("/metrics")).getStatusCode().value());

        assertEquals(200, restTemplate.getForEntity(applicationUrl("/api/meta-data"), String.class).getStatusCode().value());
        assertEquals(200, restTemplate.getForEntity(applicationUrl("/"), String.class).getStatusCode().value());
        assertEquals(200, restTemplate.getForEntity(applicationUrl("/assets/favicon.ico"), String.class).getStatusCode().value());
        assertEquals(401, getForEntityAllowingError(applicationUrl("/api/emails")).getStatusCode().value());
    }

    private String applicationUrl(String path) {
        return "http://localhost:" + applicationPort + path;
    }

    private String managementUrl(String path) {
        return "http://localhost:" + managementPort + path;
    }

    private ResponseEntity<String> getForEntityAllowingError(String url) {
        try {
            return restTemplate.getForEntity(url, String.class);
        } catch (HttpStatusCodeException ex) {
            return new ResponseEntity<>(ex.getResponseBodyAsString(), ex.getResponseHeaders(), ex.getStatusCode());
        }
    }
}
