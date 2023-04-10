package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailContent;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.contains;
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

    @BeforeEach
    void init(){
        sut.deleteAll();
    }

    @Test
    void shouldDeleteEmailsWhichExceedTheRetentionLimitOfMaximumNumberOfEmails(){
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
    void shouldNotDeleteAnyEmailWhenTheNumberOfEmailsDoesNotExceedTheRetentionLimitOfMaximumNumberOfEmails(){
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
    void shouldRetrieveEmailByMessageId(){
        var mail = createRandomEmail(5);
        var mail2 = createRandomEmail(4);
        var mail3 = createRandomEmail(3);
        
        var mailsStored = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(mailsStored, hasSize(3));
        assertThat(mailsStored, contains(mail3, mail2, mail));

        var mailResult = sut.searchEmailByMessageId(mail.getMessageId());
        assertEquals(mail, mailResult);
    }

    @Test
    void shouldNotRetrieveEmailByMessageId(){
        var mail = createRandomEmail(5);
        var mail2 = createRandomEmail(4);
        var mail3 = createRandomEmail(3);
        
        var mailsStored = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(mailsStored, hasSize(3));
        assertThat(mailsStored, contains(mail3, mail2, mail));

        var mailResult = sut.searchEmailByMessageId("<any-message-id>");
        assertEquals(null, mailResult);
    }

    private Email createRandomEmail(int minusMinutes) {
        var randomToken = RandomStringUtils.randomAlphanumeric(6);
        var localDateTime = LocalDateTime.now().minusMinutes(minusMinutes);
        var receivedOn = Date.from(localDateTime.atZone(ZoneOffset.systemDefault()).toInstant());

        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content "+randomToken);

        var mail = new Email();
        mail.setSubject("Test Subject "+randomToken);
        mail.setRawData("Test Content "+randomToken);
        mail.setReceivedOn(receivedOn);
        mail.setFromAddress("sender@example.com");
        mail.setToAddress("receiver@example.com");
        mail.addContent(content);
        mail.setMessageId(randomToken);
        return sut.save(mail);
    }

}