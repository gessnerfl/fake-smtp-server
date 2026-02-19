package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "fakesmtp.webapp.auth.username=testuser",
    "fakesmtp.webapp.auth.password=testpass"
})
class SecurityConfigAuthenticationEnabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
}
