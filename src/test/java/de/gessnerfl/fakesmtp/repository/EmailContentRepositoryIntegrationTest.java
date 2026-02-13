package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailContent;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
@ActiveProfiles("mockserver")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class EmailContentRepositoryIntegrationTest {

    @Autowired
    private EmailContentRepository sut;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private EmailInlineImageRepository emailInlineImageRepository;

    @BeforeEach
    void init() {
        emailAttachmentRepository.deleteAllInBatch();
        emailInlineImageRepository.deleteAllInBatch();
        sut.deleteAllInBatch();
        emailRepository.deleteAllInBatch();
    }

    @Test
    void shouldSaveAndLoadEmailContent() {
        var email = createEmail();
        var content = createEmailContent();
        email.addContent(content);
        emailRepository.save(email);

        var savedContent = sut.findAll();
        assertThat(savedContent, hasSize(1));
        assertEquals(ContentType.PLAIN, savedContent.getFirst().getContentType());
        assertEquals("Test Content", savedContent.getFirst().getData());
    }

    @Test
    void shouldDeleteEmailContentWhenParentEmailIsDeleted() {
        var email = createEmail();
        var content = createEmailContent();
        email.addContent(content);
        var savedEmail = emailRepository.save(email);

        assertThat(sut.findAll(), hasSize(1));

        // In Hibernate 7 without cascading, we need to delete the parent email
        // The repository method deleteEmailsExceedingDateRetentionLimitWithCascade
        // handles the cascading manually
        emailRepository.delete(savedEmail);

        assertThat(sut.findAll(), empty());
    }

    @Test
    void shouldDeleteEmailContentInBatch() {
        var email1 = createEmail();
        var content1 = createEmailContent();
        email1.addContent(content1);
        emailRepository.save(email1);

        var email2 = createEmail();
        var content2 = createEmailContent();
        email2.addContent(content2);
        emailRepository.save(email2);

        assertThat(sut.findAll(), hasSize(2));

        sut.deleteAllInBatch();

        assertThat(sut.findAll(), empty());
    }

    @Test
    void shouldVerifyEmailRelationship() {
        var email = createEmail();
        var content = createEmailContent();
        email.addContent(content);
        var savedEmail = emailRepository.save(email);

        var savedContent = sut.findAll().getFirst();
        assertNotNull(savedContent.getEmail());
        assertEquals(savedEmail.getId(), savedContent.getEmail().getId());
    }

    private Email createEmail() {
        var randomToken = RandomStringUtils.insecure().nextAlphanumeric(6);
        var email = new Email();
        email.setSubject("Test Subject " + randomToken);
        email.setRawData("Test Raw Data");
        email.setReceivedOn(ZonedDateTime.now(ZoneId.of("UTC")));
        email.setFromAddress("sender@example.com");
        email.setToAddress("receiver@example.com");
        email.setMessageId("<message-id-" + randomToken + ">");
        return email;
    }

    private EmailContent createEmailContent() {
        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content");
        return content;
    }
}
