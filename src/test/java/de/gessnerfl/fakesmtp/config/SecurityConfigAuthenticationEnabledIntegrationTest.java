package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("mockserver")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigAuthenticationEnabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public WebappAuthenticationProperties webappAuthenticationProperties() {
            var props = new WebappAuthenticationProperties();
            props.setUsername("testuser");
            props.setPassword("testpass");
            return props;
        }
    }

    @Test
    void shouldAllowAccessToManifestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/assets/manifest.json"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAccessToAssetsPathWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/assets/non-existing.js"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectSseWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/emails/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectApiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/emails"))
                .andExpect(status().isUnauthorized());
    }
}
