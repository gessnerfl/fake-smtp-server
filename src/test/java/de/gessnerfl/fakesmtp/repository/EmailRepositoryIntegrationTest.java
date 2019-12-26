package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailContent;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

@Transactional
@ActiveProfiles("integrationtest")
@RunWith(SpringRunner.class)
@SpringBootTest
public class EmailRepositoryIntegrationTest {

    private static final Sort SORT_DESC_BY_RECEIVED_ON = Sort.by(Sort.Direction.DESC, "receivedOn");
    @Autowired
    private EmailRepository sut;

    @Before
    public void init(){
        sut.deleteAll();
    }

    @Test
    public void shouldDeleteEmailsWhichExceedTheRetentionLimitOfMaximumNumberOfEmails(){
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
    public void shouldNotDeleteAnyEmailWhenTheNumberOfEmailsDoesNotExceedTheRetentionLimitOfMaximumNumberOfEmails(){
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
        return sut.save(mail);
    }

}