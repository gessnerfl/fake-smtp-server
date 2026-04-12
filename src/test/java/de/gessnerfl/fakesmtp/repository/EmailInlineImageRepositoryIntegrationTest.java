package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.InlineImage;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@ActiveProfiles("mockserver")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class EmailInlineImageRepositoryIntegrationTest {

    @Autowired
    private EmailInlineImageRepository sut;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailAttachmentRepository emailAttachmentRepository;

    @Autowired
    private EmailContentRepository emailContentRepository;

    @BeforeEach
    void init() {
        emailAttachmentRepository.deleteAllInBatch();
        emailContentRepository.deleteAllInBatch();
        sut.deleteAllInBatch();
        emailRepository.deleteAllInBatch();
    }

    @Test
    void shouldSaveAndLoadInlineImage() {
        var email = createEmail();
        var inlineImage = createInlineImage();
        email.addInlineImage(inlineImage);
        emailRepository.save(email);

        var savedImages = sut.findAll();
        assertThat(savedImages, hasSize(1));
        assertEquals("image-test-id", savedImages.getFirst().getContentId());
        assertEquals("image/png", savedImages.getFirst().getContentType());
    }

    @Test
    void shouldDeleteInlineImageWhenParentEmailIsDeleted() {
        var email = createEmail();
        var inlineImage = createInlineImage();
        email.addInlineImage(inlineImage);
        var savedEmail = emailRepository.save(email);

        assertThat(sut.findAll(), hasSize(1));

        // In Hibernate 7 without cascading, we need to delete the parent email
        // The repository method deleteEmailsExceedingDateRetentionLimitWithCascade
        // handles the cascading manually
        emailRepository.delete(savedEmail);

        assertThat(sut.findAll(), empty());
    }

    @Test
    void shouldDeleteInlineImagesInBatch() {
        var email1 = createEmail();
        var inlineImage1 = createInlineImage();
        email1.addInlineImage(inlineImage1);
        emailRepository.save(email1);

        var email2 = createEmail();
        var inlineImage2 = createInlineImage();
        email2.addInlineImage(inlineImage2);
        emailRepository.save(email2);

        assertThat(sut.findAll(), hasSize(2));

        sut.deleteAllInBatch();

        assertThat(sut.findAll(), empty());
    }

    @Test
    void shouldVerifyEmailRelationship() {
        var email = createEmail();
        var inlineImage = createInlineImage();
        email.addInlineImage(inlineImage);
        var savedEmail = emailRepository.save(email);

        var savedImage = sut.findAll().getFirst();
        assertNotNull(savedImage.getEmail());
        assertEquals(savedEmail.getId(), savedImage.getEmail().getId());
    }

    @Test
    void shouldStoreBase64EncodedImageData() {
        var email = createEmail();
        var inlineImage = createInlineImage();
        email.addInlineImage(inlineImage);
        emailRepository.save(email);

        var savedImage = sut.findAll().getFirst();
        var expectedData = Base64.getEncoder().encodeToString("fake-image-data".getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedData, savedImage.getData());
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

    private InlineImage createInlineImage() {
        var inlineImage = new InlineImage();
        inlineImage.setContentId("image-test-id");
        inlineImage.setContentType("image/png");
        inlineImage.setData(Base64.getEncoder().encodeToString("fake-image-data".getBytes(StandardCharsets.UTF_8)));
        return inlineImage;
    }
}
