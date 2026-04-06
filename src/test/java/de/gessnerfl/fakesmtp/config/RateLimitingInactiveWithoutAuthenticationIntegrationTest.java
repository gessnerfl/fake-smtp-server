package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("mockserver")
@SpringBootTest(properties = {
        "fakesmtp.webapp.authentication.enabled=false",
        "fakesmtp.webapp.authentication.username=",
        "fakesmtp.webapp.authentication.password=",
        "fakesmtp.webapp.rate-limiting.enabled=true",
        "fakesmtp.webapp.rate-limiting.max-attempts=1"
})
@AutoConfigureMockMvc
class RateLimitingInactiveWithoutAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldNotExposeRateLimitingHeadersWhenWebAuthenticationIsDisabled() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "testuser")
                        .param("password", "wrongpass"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("X-RateLimit-Remaining"))
                .andExpect(header().doesNotExist("Retry-After"));
    }
}
