package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.EmailBuilder;
import de.gessnerfl.fakesmtp.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.transaction.Transactional;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@ActiveProfiles("integrationtest")
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

    private Email createRandomEmail(int minusMinutes) {        
        return sut.save(new EmailBuilder().receivedMinutesAgo(minusMinutes).build());
    }

}