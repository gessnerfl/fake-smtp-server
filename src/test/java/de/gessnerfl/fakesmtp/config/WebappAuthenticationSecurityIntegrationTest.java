package de.gessnerfl.fakesmtp.config;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailContentRepository;
import de.gessnerfl.fakesmtp.repository.EmailInlineImageRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("mockserver")
@SpringBootTest(properties = {
        "fakesmtp.webapp.authentication.username=testuser",
        "fakesmtp.webapp.authentication.password=testpass"
})
@AutoConfigureMockMvc
class WebappAuthenticationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private EmailContentRepository emailContentRepository;

    @Autowired
    private EmailInlineImageRepository emailInlineImageRepository;

    @BeforeEach
    void setUp() {
        emailAttachmentRepository.deleteAllInBatch();
        emailContentRepository.deleteAllInBatch();
        emailInlineImageRepository.deleteAllInBatch();
        emailRepository.deleteAllInBatch();
    }

    @Test
    void shouldRejectApiRequestsWithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/emails"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectApiRequestsWithBasicAuthorizationHeader() throws Exception {
        String basicAuthValue = Base64.getEncoder().encodeToString("testuser:testpass".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/emails")
                        .header("Authorization", "Basic " + basicAuthValue))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowApiRequestsWithValidCredentials() throws Exception {
        // Get CSRF token from meta-data endpoint and create a new session
        MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        
        String csrfToken = extractCsrfTokenFromCookies(metaResult);
        jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
        MockHttpSession session = new MockHttpSession();

        // Login with CSRF token and new session
        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(session)
                        .param("username", "testuser")
                        .param("password", "testpass")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession loginSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/api/emails")
                        .session(loginSession))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectEmailDetailsWithoutCredentials() throws Exception {
        Email email = emailRepository.saveAndFlush(buildEmail("secret-subject", "secret@example.com"));

        mockMvc.perform(get("/api/emails/" + email.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectEmailAttachmentsWithoutCredentials() throws Exception {
        Email email = emailRepository.saveAndFlush(buildEmail("secret-subject", "secret@example.com"));
        Long attachmentId = email.getAttachments().get(0).getId();

        mockMvc.perform(get("/api/emails/" + email.getId() + "/attachments/" + attachmentId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotExposeEmailDataInUiShellWithoutCredentials() throws Exception {
        Email email = emailRepository.saveAndFlush(buildEmail("secret-subject", "secret@example.com"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(email.getSubject()))))
                .andExpect(content().string(not(containsString(email.getToAddress()))));
    }

    @Test
    void shouldAllowMetaDataWithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectDeleteEmailByIdWithoutCredentials() throws Exception {
        Email email = emailRepository.saveAndFlush(buildEmail("test-subject", "test@example.com"));

        // CSRF is checked before authentication for state-changing requests
        mockMvc.perform(delete("/api/emails/" + email.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDeleteAllEmailsWithoutCredentials() throws Exception {
        emailRepository.saveAndFlush(buildEmail("test-subject-1", "test1@example.com"));
        emailRepository.saveAndFlush(buildEmail("test-subject-2", "test2@example.com"));

        // CSRF is checked before authentication for state-changing requests
        mockMvc.perform(delete("/api/emails"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDeleteEmailByIdWithoutCsrfToken() throws Exception {
        Email email = emailRepository.saveAndFlush(buildEmail("test-subject", "test@example.com"));

        // Get CSRF token from meta-data endpoint and create a new session
        MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        
        String csrfToken = extractCsrfTokenFromCookies(metaResult);
        jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
        MockHttpSession session = new MockHttpSession();

        // Login with CSRF token and new session
        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(session)
                        .param("username", "testuser")
                        .param("password", "testpass")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession loginSession = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Try to delete without CSRF token - should fail with 403
        mockMvc.perform(delete("/api/emails/" + email.getId())
                        .session(loginSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDeleteAllEmailsWithoutCsrfToken() throws Exception {
        emailRepository.saveAndFlush(buildEmail("test-subject-1", "test1@example.com"));
        emailRepository.saveAndFlush(buildEmail("test-subject-2", "test2@example.com"));

        // Get CSRF token from meta-data endpoint and create a new session
        MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        
        String csrfToken = extractCsrfTokenFromCookies(metaResult);
        jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
        MockHttpSession session = new MockHttpSession();

        // Login with CSRF token and new session
        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(session)
                        .param("username", "testuser")
                        .param("password", "testpass")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession loginSession = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Try to delete without CSRF token - should fail with 403
        mockMvc.perform(delete("/api/emails")
                        .session(loginSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteEmailByIdWithValidCredentialsAndCsrfToken() throws Exception {
        Email email = emailRepository.saveAndFlush(buildEmail("test-subject", "test@example.com"));

        // Get CSRF token from meta-data endpoint and create a new session
        MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        
        String csrfToken = extractCsrfTokenFromCookies(metaResult);
        jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
        MockHttpSession session = new MockHttpSession();

        // Login with CSRF token and new session
        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(session)
                        .param("username", "testuser")
                        .param("password", "testpass")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession loginSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        
        // Get new CSRF token after login (it is regenerated by success handler)
        String newCsrfToken = extractCsrfTokenFromCookies(loginResult);
        jakarta.servlet.http.Cookie newCsrfCookie = extractCsrfCookie(loginResult);
        
        // Delete with new CSRF token (both as cookie and header) and session
        mockMvc.perform(delete("/api/emails/" + email.getId())
                        .session(loginSession)
                        .cookie(newCsrfCookie)
                        .header("X-XSRF-TOKEN", newCsrfToken))
                .andExpect(status().isOk());

        assertTrue(emailRepository.findById(email.getId()).isEmpty(), "Email should be deleted");
    }

    @Test
    void shouldDeleteAllEmailsWithValidCredentialsAndCsrfToken() throws Exception {
        emailRepository.saveAndFlush(buildEmail("test-subject-1", "test1@example.com"));
        emailRepository.saveAndFlush(buildEmail("test-subject-2", "test2@example.com"));
        assertEquals(2, emailRepository.count(), "Should have 2 emails before deletion");

        // Get CSRF token from meta-data endpoint and create a new session
        MvcResult metaResult = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        
        String csrfToken = extractCsrfTokenFromCookies(metaResult);
        jakarta.servlet.http.Cookie csrfCookie = extractCsrfCookie(metaResult);
        MockHttpSession session = new MockHttpSession();

        // Login with CSRF token and new session
        MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .session(session)
                        .param("username", "testuser")
                        .param("password", "testpass")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession loginSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        
        // Get new CSRF token after login (it is regenerated by success handler)
        String newCsrfToken = extractCsrfTokenFromCookies(loginResult);
        jakarta.servlet.http.Cookie newCsrfCookie = extractCsrfCookie(loginResult);

        // Delete all with new CSRF token (both as cookie and header) and session
        mockMvc.perform(delete("/api/emails")
                        .session(loginSession)
                        .cookie(newCsrfCookie)
                        .header("X-XSRF-TOKEN", newCsrfToken))
                .andExpect(status().isOk());

        assertEquals(0, emailRepository.count(), "All emails should be deleted");
    }

    private String extractCsrfTokenFromCookies(MvcResult result) {
        jakarta.servlet.http.Cookie cookie = extractCsrfCookie(result);
        return cookie != null ? cookie.getValue() : "";
    }

    private jakarta.servlet.http.Cookie extractCsrfCookie(MvcResult result) {
        if (result.getResponse().getCookies() != null) {
            // Find the non-empty XSRF-TOKEN cookie (the new one after login)
            // There may be a deleted cookie (empty value) and a new one
            jakarta.servlet.http.Cookie validCookie = null;
            for (jakarta.servlet.http.Cookie cookie : result.getResponse().getCookies()) {
                if ("XSRF-TOKEN".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                    validCookie = cookie;
                }
            }
            
            if (validCookie != null) {
                // Return a copy of the cookie to avoid reference issues
                jakarta.servlet.http.Cookie copy = new jakarta.servlet.http.Cookie(validCookie.getName(), validCookie.getValue());
                copy.setPath(validCookie.getPath() != null ? validCookie.getPath() : "/");
                copy.setMaxAge(validCookie.getMaxAge());
                copy.setSecure(validCookie.getSecure());
                return copy;
            }
        }
        return null;
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
