package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.model.InlineImage;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@ActiveProfiles("mockserver")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class EmailRepositoryIntegrationTest {

    private static final Sort SORT_DESC_BY_RECEIVED_ON = Sort.by(Sort.Direction.DESC, "receivedOn");
    @Autowired
    private EmailRepository sut;

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private EmailContentRepository emailContentRepository;

    @Autowired
    private EmailInlineImageRepository emailInlineImageRepository;

    @BeforeEach
    void init() {
        emailAttachmentRepository.deleteAllInBatch();
        emailContentRepository.deleteAllInBatch();
        emailInlineImageRepository.deleteAllInBatch();
        sut.deleteAllInBatch();
    }

    @Test
    void shouldDeleteEmailsWhichExceedTheRetentionLimitOfMaximumNumberOfEmails() {
        var mail1 = createRandomEmail(5);
        var mail2 = createRandomEmail(4);
        var mail3 = createRandomEmail(3);
        var mail4 = createRandomEmail(2);
        var mail5 = createRandomEmail(1);

        var beforeDeletion = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(beforeDeletion, hasSize(5));
        assertThat(beforeDeletion, contains(mail5, mail4, mail3, mail2, mail1));

        var count = sut.deleteEmailsExceedingDateRetentionLimit(3);
        assertEquals(2, count);

        var afterDeletion = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(afterDeletion, hasSize(3));
        assertThat(afterDeletion, contains(mail5, mail4, mail3));
    }

    @Test
    void shouldNotDeleteAnyEmailWhenTheNumberOfEmailsDoesNotExceedTheRetentionLimitOfMaximumNumberOfEmails() {
        var mail1 = createRandomEmail(5);
        var mail2 = createRandomEmail(4);
        var mail3 = createRandomEmail(3);

        var beforeDeletion = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(beforeDeletion, hasSize(3));
        assertThat(beforeDeletion, contains(mail3, mail2, mail1));

        var count = sut.deleteEmailsExceedingDateRetentionLimit(3);
        assertEquals(0, count);

        var afterDeletion = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(afterDeletion, hasSize(3));
        assertThat(beforeDeletion, contains(mail3, mail2, mail1));
    }

    @Test
    void shouldDeleteEmailsWithAllChildEntitiesWhenExceedingLimit() {
        // Create 5 emails - oldest ones (mail1, mail2) should be deleted
        createRandomEmailWithAllChildren(5);
        createRandomEmailWithAllChildren(4);
        var mail3 = createRandomEmailWithAllChildren(3);
        var mail4 = createRandomEmailWithAllChildren(2);
        var mail5 = createRandomEmailWithAllChildren(1);

        assertThat(sut.findAll(SORT_DESC_BY_RECEIVED_ON), hasSize(5));
        assertThat(emailAttachmentRepository.findAll(), hasSize(5));
        assertThat(emailContentRepository.findAll(), hasSize(5));
        assertThat(emailInlineImageRepository.findAll(), hasSize(5));

        var count = sut.deleteEmailsExceedingDateRetentionLimit(3);
        assertEquals(2, count);

        assertThat(sut.findAll(SORT_DESC_BY_RECEIVED_ON), hasSize(3));
        assertThat(sut.findAll(SORT_DESC_BY_RECEIVED_ON), contains(mail5, mail4, mail3));
        assertThat(emailAttachmentRepository.findAll(), hasSize(3));
        assertThat(emailContentRepository.findAll(), hasSize(3));
        assertThat(emailInlineImageRepository.findAll(), hasSize(3));
    }

    @Test
    void shouldDeleteSingleEmailWithCascade() {
        var mail = createRandomEmailWithAllChildren(1);

        assertThat(sut.findAll(), hasSize(1));
        assertThat(emailAttachmentRepository.findAll(), hasSize(1));
        assertThat(emailContentRepository.findAll(), hasSize(1));
        assertThat(emailInlineImageRepository.findAll(), hasSize(1));

        sut.deleteById(mail.getId());

        assertThat(sut.findAll(), empty());
        assertThat(emailAttachmentRepository.findAll(), empty());
        assertThat(emailContentRepository.findAll(), empty());
        assertThat(emailInlineImageRepository.findAll(), empty());
    }

    private Email createRandomEmail(int minusMinutes) {
        var randomToken = RandomStringUtils.insecure().nextAlphanumeric(6);
        var receivedOn = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(minusMinutes);

        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content " + randomToken);

        var mail = new Email();
        mail.setSubject("Test Subject " + randomToken);
        mail.setRawData("Test Content " + randomToken);
        mail.setReceivedOn(receivedOn);
        mail.setFromAddress("sender@example.com");
        mail.setToAddress("receiver@example.com");
        mail.addContent(content);
        mail.setMessageId(randomToken);
        return sut.save(mail);
    }

    private Email createRandomEmailWithAllChildren(int minusMinutes) {
        var randomToken = RandomStringUtils.insecure().nextAlphanumeric(6);
        var receivedOn = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(minusMinutes);

        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content " + randomToken);

        var attachment = new EmailAttachment();
        attachment.setFilename("test-" + randomToken + ".txt");
        attachment.setData("Attachment data " .getBytes(StandardCharsets.UTF_8));

        var inlineImage = new InlineImage();
        inlineImage.setContentId("image-" + randomToken);
        inlineImage.setContentType("image/png");
        inlineImage.setData(Base64.getEncoder().encodeToString("fake-image-data".getBytes(StandardCharsets.UTF_8)));

        var mail = new Email();
        mail.setSubject("Test Subject " + randomToken);
        mail.setRawData("Test Content " + randomToken);
        mail.setReceivedOn(receivedOn);
        mail.setFromAddress("sender@example.com");
        mail.setToAddress("receiver@example.com");
        mail.setMessageId(randomToken);
        mail.addContent(content);
        mail.addAttachment(attachment);
        mail.addInlineImage(inlineImage);
        return sut.save(mail);
    }

}
