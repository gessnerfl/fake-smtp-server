package de.gessnerfl.fakesmtp.smtp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.RestResponsePage;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailContentRepository;
import de.gessnerfl.fakesmtp.repository.EmailInlineImageRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.smtp.server.SmtpServer;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Integration Test for the complete SMTP email flow.
 * Tests the entire chain: JavaMail Client -> SMTP Server -> MessageListener -> Repository -> REST API
 */
@Transactional
@ActiveProfiles({"integrationtest", "default"})
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SmtpEmailEndToEndIntegrationTest {

    private static final String FROM_ADDRESS = "sender@example.com";
    private static final String TO_ADDRESS = "receiver@example.com";
    private static final String SUBJECT = "Test Email Subject";
    private static final String BODY_TEXT = "This is the test email body content.";
    private static final String HTML_BODY = "<html><body><h1>Test Email</h1><p>This is HTML content.</p></body></html>";
    private static final String ATTACHMENT_FILENAME = "test-attachment.txt";
    private static final String ATTACHMENT_CONTENT = "This is attachment content.";

    @LocalServerPort
    private int serverPort;

    @Autowired
    private SmtpServer smtpServer;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private EmailContentRepository emailContentRepository;

    @Autowired
    private EmailInlineImageRepository emailInlineImageRepository;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    private int emailCounter = 0;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        clearEmailData();

        emailCounter = 0;
    }

    @AfterEach
    void tearDown() {
        clearEmailData();
    }

    private void clearEmailData() {
        emailAttachmentRepository.deleteAllInBatch();
        emailContentRepository.deleteAllInBatch();
        emailInlineImageRepository.deleteAllInBatch();
        emailRepository.deleteAllInBatch();
        emailRepository.flush();
    }

    @Test
    void shouldReceiveSimpleTextEmailViaSmtpAndMakeItAvailableViaRestApi() throws Exception {
        // Given: Send a simple text email via JavaMail with unique subject
        String uniqueSubject = SUBJECT + " Simple Text " + System.currentTimeMillis();
        sendTextEmail(FROM_ADDRESS, TO_ADDRESS, uniqueSubject, BODY_TEXT);

        // Then: Wait for and verify email is stored in database
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var emails = emailRepository.findBySubject(uniqueSubject);
            assertThat(emails, hasSize(1));
        });

        // And: Verify email is accessible via REST API
        var emails = fetchEmailsFromApi();
        Email receivedEmail = emails.getContent().stream()
                .filter(e -> e.getSubject().equals(uniqueSubject))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Email with subject '" + uniqueSubject + "' not found"));

        assertEquals(FROM_ADDRESS, receivedEmail.getFromAddress());
        assertEquals(TO_ADDRESS, receivedEmail.getToAddress());
        assertEquals(uniqueSubject, receivedEmail.getSubject());
        assertTrue(receivedEmail.getPlainContent().isPresent());
        assertThat(receivedEmail.getPlainContent().get().getData(), containsString(BODY_TEXT));
        assertNotNull(receivedEmail.getReceivedOn());
        assertNotNull(receivedEmail.getMessageId());
    }

    @Test
    void shouldReceiveHtmlEmailViaSmtpAndMakeItAvailableViaRestApi() throws Exception {
        // Given: Send an HTML email with unique subject
        String uniqueSubject = SUBJECT + " HTML " + System.currentTimeMillis();
        sendHtmlEmail(FROM_ADDRESS, TO_ADDRESS, uniqueSubject, HTML_BODY);

        // Then: Wait for email to be stored
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var emails = emailRepository.findBySubject(uniqueSubject);
            assertThat(emails, hasSize(1));
        });

        // And: Verify via REST API
        var emails = fetchEmailsFromApi();
        Email receivedEmail = emails.getContent().stream()
                .filter(e -> e.getSubject().equals(uniqueSubject))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Email with subject '" + uniqueSubject + "' not found"));

        assertEquals(FROM_ADDRESS, receivedEmail.getFromAddress());
        assertEquals(TO_ADDRESS, receivedEmail.getToAddress());
        assertEquals(uniqueSubject, receivedEmail.getSubject());
        assertTrue(receivedEmail.getHtmlContent().isPresent());
        assertEquals(HTML_BODY, receivedEmail.getHtmlContent().get().getData());
    }

    @Test
    void shouldReceiveEmailWithAttachmentViaSmtpAndMakeItAvailableViaRestApi() throws Exception {
        // Given: Send an email with attachment with unique subject
        String uniqueSubject = SUBJECT + " With Attachment " + System.currentTimeMillis();
        sendEmailWithAttachment(FROM_ADDRESS, TO_ADDRESS, uniqueSubject, BODY_TEXT, ATTACHMENT_FILENAME, ATTACHMENT_CONTENT);

        // Then: Wait for email to be stored (attachments will be loaded via REST API)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var emails = emailRepository.findBySubject(uniqueSubject);
            assertThat(emails, hasSize(1));
        });

        // And: Verify via REST API (attachments are loaded eagerly in REST response)
        var emails = fetchEmailsFromApi();
        Email receivedEmail = emails.getContent().stream()
                .filter(e -> e.getSubject().equals(uniqueSubject))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Email with subject '" + uniqueSubject + "' not found"));

        assertEquals(uniqueSubject, receivedEmail.getSubject());
        assertThat(receivedEmail.getAttachments(), hasSize(1));
        assertEquals(ATTACHMENT_FILENAME, receivedEmail.getAttachments().getFirst().getFilename());

        // Verify attachment content via API
        Long emailId = receivedEmail.getId();
        Long attachmentId = receivedEmail.getAttachments().getFirst().getId();
        byte[] attachmentData = fetchAttachmentFromApi(emailId, attachmentId);
        assertEquals(ATTACHMENT_CONTENT, new String(attachmentData, StandardCharsets.UTF_8));
    }

    @Test
    void shouldReceiveMultipartEmailWithTextAndHtmlViaSmtp() throws Exception {
        // Given: Send a multipart email with both text and HTML with unique subject
        String uniqueSubject = SUBJECT + " Multipart " + System.currentTimeMillis();
        sendMultipartEmail(FROM_ADDRESS, TO_ADDRESS, uniqueSubject, BODY_TEXT, HTML_BODY);

        // Then: Wait for email to be stored
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var emails = emailRepository.findBySubject(uniqueSubject);
            assertThat(emails, hasSize(1));
        });

        // And: Verify both content types are available
        var emails = fetchEmailsFromApi();
        Email receivedEmail = emails.getContent().stream()
                .filter(e -> e.getSubject().equals(uniqueSubject))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Email with subject '" + uniqueSubject + "' not found"));

        assertTrue(receivedEmail.getPlainContent().isPresent(), "Plain text content should be present");
        assertTrue(receivedEmail.getHtmlContent().isPresent(), "HTML content should be present");
        assertThat(receivedEmail.getPlainContent().get().getData(), containsString(BODY_TEXT));
        assertEquals(HTML_BODY, receivedEmail.getHtmlContent().get().getData());
    }

    @Test
    void shouldReceiveMultipleEmailsViaSmtp() throws Exception {
        // Given: Send multiple emails with unique subjects
        int emailCount = 5;
        long timestamp = System.currentTimeMillis();
        for (int i = 0; i < emailCount; i++) {
            String uniqueSubject = SUBJECT + " Multiple " + timestamp + " " + i;
            sendTextEmail(FROM_ADDRESS, TO_ADDRESS, uniqueSubject, BODY_TEXT + " " + i);
        }

        // Then: Wait for all emails to be stored
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            int foundCount = 0;
            for (int i = 0; i < emailCount; i++) {
                String uniqueSubject = SUBJECT + " Multiple " + timestamp + " " + i;
                var emails = emailRepository.findBySubject(uniqueSubject);
                if (emails.size() == 1) {
                    foundCount++;
                }
            }
            assertEquals(emailCount, foundCount);
        });

        // And: Verify all are accessible via REST API
        var allEmails = fetchEmailsFromApi();
        for (int i = 0; i < emailCount; i++) {
            String uniqueSubject = SUBJECT + " Multiple " + timestamp + " " + i;
            Email receivedEmail = allEmails.getContent().stream()
                    .filter(e -> e.getSubject().equals(uniqueSubject))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Email with subject '" + uniqueSubject + "' not found"));
            assertThat(receivedEmail.getPlainContent().get().getData(), containsString(BODY_TEXT + " " + i));
        }
    }

    @Test
    void shouldReceiveEmailWithSpecialCharactersViaSmtp() throws Exception {
        // Given: Send an email with special characters and UTF-8 content with unique subject
        String timestamp = String.valueOf(System.currentTimeMillis());
        String specialSubject = "Test: äöü ÄÖÜ ß € Special Chars " + timestamp;
        String specialBody = "Content with unicode: 日本語 한국어 中文 🎉";

        sendTextEmail(FROM_ADDRESS, TO_ADDRESS, specialSubject, specialBody);

        // Then: Wait for email to be stored
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var emails = emailRepository.findBySubject(specialSubject);
            assertThat(emails, hasSize(1));
        });

        // And: Verify content is correctly preserved
        var emails = fetchEmailsFromApi();
        Email receivedEmail = emails.getContent().stream()
                .filter(e -> e.getSubject().equals(specialSubject))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Email with subject '" + specialSubject + "' not found"));

        assertEquals(specialSubject, receivedEmail.getSubject());
        assertTrue(receivedEmail.getPlainContent().isPresent());
        assertThat(receivedEmail.getPlainContent().get().getData(), containsString(specialBody));
    }

    // Helper methods for sending emails

    private void sendTextEmail(String from, String to, String subject, String body) throws MessagingException {
        Properties props = getMailProperties();
        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject);
        message.setText(body);
        message.setSentDate(new Date());

        Transport.send(message);
    }

    private void sendHtmlEmail(String from, String to, String subject, String htmlBody) throws MessagingException {
        Properties props = getMailProperties();
        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=utf-8");
        message.setSentDate(new Date());

        Transport.send(message);
    }

    private void sendEmailWithAttachment(String from, String to, String subject, String body, String attachmentName, String attachmentContent) throws MessagingException {
        Properties props = getMailProperties();
        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject);
        message.setSentDate(new Date());

        // Create multipart message
        Multipart multipart = new MimeMultipart();

        // Add text part
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body);
        multipart.addBodyPart(textPart);

        // Add attachment part
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setText(attachmentContent);
        attachmentPart.setFileName(attachmentName);
        multipart.addBodyPart(attachmentPart);

        message.setContent(multipart);
        Transport.send(message);
    }

    private void sendMultipartEmail(String from, String to, String subject, String textBody, String htmlBody) throws MessagingException {
        Properties props = getMailProperties();
        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject);
        message.setSentDate(new Date());

        // Create multipart/alternative message
        Multipart multipart = new MimeMultipart("alternative");

        // Add plain text part
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(textBody, "utf-8");
        multipart.addBodyPart(textPart);

        // Add HTML part
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=utf-8");
        multipart.addBodyPart(htmlPart);

        message.setContent(multipart);
        Transport.send(message);
    }

    private Properties getMailProperties() {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", String.valueOf(smtpServer.getPort()));
        props.setProperty("mail.smtp.sendpartial", "true");
        props.setProperty("mail.mime.charset", "UTF-8");
        return props;
    }

    // Helper methods for REST API calls

    private RestResponsePage<Email> fetchEmailsFromApi() throws IOException {
        String url = "http://localhost:" + serverPort + "/api/emails";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(200, response.getStatusCode().value());
        return objectMapper.readValue(response.getBody(), new TypeReference<RestResponsePage<Email>>() {});
    }

    private byte[] fetchAttachmentFromApi(Long emailId, Long attachmentId) {
        String url = "http://localhost:" + serverPort + "/api/emails/" + emailId + "/attachments/" + attachmentId;
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        assertEquals(200, response.getStatusCode().value());
        return response.getBody();
    }
}
