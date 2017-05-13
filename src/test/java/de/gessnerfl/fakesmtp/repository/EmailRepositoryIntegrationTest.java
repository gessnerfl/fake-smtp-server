package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.Email;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

@ActiveProfiles("integrationtest")
@RunWith(SpringRunner.class)
@SpringBootTest
public class EmailRepositoryIntegrationTest {

    public static final Sort SORT_DESC_BY_RECEIVED_ON = new Sort(Sort.Direction.DESC, "receivedOn");
    @Autowired
    private EmailRepository sut;

    @Before
    public void init(){
        sut.deleteAll();
    }

    @Test
    public void shouldDeleteEmailsWhichExceedTheRetentionLimitOfMaximumNumberOfEmails(){
        Email mail1 = createRandomEmail(5);
        Email mail2 = createRandomEmail(4);
        Email mail3 = createRandomEmail(3);
        Email mail4 = createRandomEmail(2);
        Email mail5 = createRandomEmail(1);

        List<Email> beforeDeletion = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(beforeDeletion, hasSize(5));
        assertThat(beforeDeletion, contains(mail5, mail4, mail3, mail2, mail1));

        int count = sut.deleteEmailsExceedingDateRetentionLimit(3);
        assertEquals(2, count);

        List<Email> afterDeletion = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(afterDeletion, hasSize(3));
        assertThat(afterDeletion, contains(mail5, mail4, mail3));
    }

    @Test
    public void shouldNotDeleteAnyEmailWhenTheNumberOfEmailsDoesNotExceedTheRetentionLimitOfMaximumNumberOfEmails(){
        Email mail1 = createRandomEmail(5);
        Email mail2 = createRandomEmail(4);
        Email mail3 = createRandomEmail(3);

        List<Email> beforeDeletion = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(beforeDeletion, hasSize(3));
        assertThat(beforeDeletion, contains(mail3, mail2, mail1));

        int count = sut.deleteEmailsExceedingDateRetentionLimit(3);
        assertEquals(0, count);

        List<Email> afterDeletion = sut.findAll(SORT_DESC_BY_RECEIVED_ON);
        assertThat(afterDeletion, hasSize(3));
        assertThat(beforeDeletion, contains(mail3, mail2, mail1));
    }

    private Email createRandomEmail(int minusMinutes) {
        final String randomToken = RandomStringUtils.randomAlphanumeric(6);
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(minusMinutes);
        Date receivedOn = Date.from(localDateTime.atZone(ZoneOffset.systemDefault()).toInstant());
        Email mail = new Email();
        mail.setSubject("Test Subject "+randomToken);
        mail.setContent("Test Content "+randomToken);
        mail.setReceivedOn(receivedOn);
        mail.setFromAddress("sender@example.com");
        mail.setToAddress("receiver@example.com");
        return sut.save(mail);
    }

}