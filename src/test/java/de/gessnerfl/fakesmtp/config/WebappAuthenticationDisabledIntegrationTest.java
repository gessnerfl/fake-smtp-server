package de.gessnerfl.fakesmtp.config;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Web UI authentication when authentication is DISABLED.
 * Ensures that all endpoints work without authentication when credentials are not configured.
 */
@ActiveProfiles("mockserver")
@SpringBootTest(properties = {
        "fakesmtp.webapp.authentication.username=",
        "fakesmtp.webapp.authentication.password="
})
@AutoConfigureMockMvc
class WebappAuthenticationDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailRepository emailRepository;

    @BeforeEach
    void setUp() {
        emailRepository.deleteAll();
    }

    @Test
    void shouldAllowAccessToApiEmailsWithoutAuthentication() throws Exception {
        emailRepository.saveAndFlush(buildEmail("test-subject", "test@example.com"));

        mockMvc.perform(get("/api/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldAllowAccessToEmailDetailsWithoutAuthentication() throws Exception {
        Email email = emailRepository.saveAndFlush(buildEmail("secret-subject", "secret@example.com"));

        mockMvc.perform(get("/api/emails/" + email.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value(email.getSubject()));
    }

    @Test
    void shouldAllowAccessToEmailAttachmentsWithoutAuthentication() throws Exception {
        Email email = emailRepository.saveAndFlush(buildEmail("secret-subject", "secret@example.com"));
        Long attachmentId = email.getAttachments().get(0).getId();

        mockMvc.perform(get("/api/emails/" + email.getId() + "/attachments/" + attachmentId))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAccessToUiShellWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAccessToEmailsRouteWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/emails"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAccessToEmailDetailRouteWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/emails/123"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAccessToMetaDataWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticationEnabled").value(false));
    }

    @Test
    void shouldAllowAccessToSseEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/emails/events"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowDeleteEmailByIdWithoutAuthentication() throws Exception {
        Email email = emailRepository.saveAndFlush(buildEmail("test-subject", "test@example.com"));

        mockMvc.perform(delete("/api/emails/" + email.getId()))
                .andExpect(status().isOk());

        assertTrue(emailRepository.findById(email.getId()).isEmpty(), "Email should be deleted");
    }

    @Test
    void shouldAllowDeleteAllEmailsWithoutAuthentication() throws Exception {
        emailRepository.saveAndFlush(buildEmail("test-subject-1", "test1@example.com"));
        emailRepository.saveAndFlush(buildEmail("test-subject-2", "test2@example.com"));
        assertEquals(2, emailRepository.count(), "Should have 2 emails before deletion");

        mockMvc.perform(delete("/api/emails"))
                .andExpect(status().isOk());

        assertEquals(0, emailRepository.count(), "All emails should be deleted");
    }

    @Test
    void shouldAllowAccessToStaticAssetsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/assets/favicon.ico"))
                .andExpect(status().isOk());
    }

    private static Email buildEmail(String subject, String toAddress) {
        Email email = new Email();
        email.setFromAddress("sender@example.com");
        email.setToAddress(toAddress);
        email.setSubject(subject);
        email.setReceivedOn(ZonedDateTime.now(ZoneId.of("UTC")));
        email.setRawData("raw");

        EmailContent content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("body");
        email.addContent(content);

        EmailAttachment attachment = new EmailAttachment();
        attachment.setFilename("test.txt");
        attachment.setData("test".getBytes(StandardCharsets.UTF_8));
        email.addAttachment(attachment);
        return email;
    }
}
