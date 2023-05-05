package de.gessnerfl.fakesmtp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.gessnerfl.fakesmtp.repository.EmailRepository;
import jakarta.transaction.Transactional;

@Transactional
@ActiveProfiles("mockserver")
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class EmailSpecificationTest {

    @Autowired
    private EmailRepository emailRepository;

    @BeforeEach
    void init(){
        emailRepository.deleteAll();
    }

    @Test
    public void shouldSearchByToAddressEquality() {
        var email1 = createEmail("subject", "raw data", "from@address.domain", "friend@address.domain", "message-id", 1);
        createEmail("subject", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);

        EmailSpecification spec = new EmailSpecification(new SpecSearchCriteria("fromAddress", SearchOperation.EQUALITY, "from@address.domain"));
        List<Email> result = emailRepository.findAll(Specification.where(spec));

        assertEquals(result.size(), 1);
        assertEquals(email1, result.get(0));
    }

    @Test
    public void shouldSearchByContainsSubject() {
        var email1 = createEmail("this is subject", "raw data", "from@address.domain", "friend@address.domain", "message-id", 1);
        createEmail("another sub", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);
        var email2 = createEmail("le subject", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);
        createEmail("hello", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);

        EmailSpecification spec = new EmailSpecification(new SpecSearchCriteria("subject", SearchOperation.CONTAINS, "subject"));
        List<Email> result = emailRepository.findAll(Specification.where(spec));

        assertEquals(result.size(), 2);
        assertEquals(email1, result.get(0));
        assertEquals(email2, result.get(1));
    }

    @Test
    public void shouldSearchByNegationSubject() {
        var email1 = createEmail("this is subject", "raw data", "from@address.domain", "friend@address.domain", "message-id", 1);
        var email2 = createEmail("another sub", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);
        createEmail("subject", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);

        EmailSpecification spec = new EmailSpecification(new SpecSearchCriteria("subject", SearchOperation.NEGATION, "subject"));
        List<Email> result = emailRepository.findAll(Specification.where(spec));

        assertEquals(result.size(), 2);
        assertEquals(email1, result.get(0));
        assertEquals(email2, result.get(1));
    }

    @Test
    public void shouldSearchByEndsWithSubject() {
        var email1 = createEmail("this is subject", "raw data", "from@address.domain", "friend@address.domain", "message-id", 1);
        var email2 = createEmail("another project", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);
        createEmail("another sub", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);

        EmailSpecification spec = new EmailSpecification(new SpecSearchCriteria("subject", SearchOperation.ENDS_WITH, "ject"));
        List<Email> result = emailRepository.findAll(Specification.where(spec));

        assertEquals(result.size(), 2);
        assertEquals(email1, result.get(0));
        assertEquals(email2, result.get(1));
    }

    @Test
    public void shouldSearchByStartWithSubject() {
        createEmail("this is subject", "raw data", "from@address.domain", "friend@address.domain", "message-id", 1);
        var email1 = createEmail("another project", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);
        var email2 = createEmail("an other sub", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);

        EmailSpecification spec = new EmailSpecification(new SpecSearchCriteria("subject", SearchOperation.STARTS_WITH, "an"));
        List<Email> result = emailRepository.findAll(Specification.where(spec));

        assertEquals(result.size(), 2);
        assertEquals(email1, result.get(0));
        assertEquals(email2, result.get(1));
    }

    @Test
    public void shouldSearchByGreaterThanReceivedOn() {
        createEmail("this is subject", "raw data", "from@address.domain", "friend@address.domain", "message-id", 15);
        var email1 = createEmail("another project", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 10);
        var email2 = createEmail("another sub", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);

        var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        EmailSpecification spec = new EmailSpecification(new SpecSearchCriteria("receivedOn", SearchOperation.GREATER_THAN, 
            simpleDateFormat.format(email1.getReceivedOn())));
        List<Email> result = emailRepository.findAll(Specification.where(spec));

        assertEquals(result.size(), 1);
        assertEquals(email2, result.get(0));
    }

    @Test
    public void shouldSearchByLessThanReceivedOn() {
        var email1 = createEmail("this is subject", "raw data", "from@address.domain", "friend@address.domain", "message-id", 15);
        var email2 = createEmail("another project", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 10);
        createEmail("another sub", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 5);

        var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        EmailSpecification spec = new EmailSpecification(new SpecSearchCriteria("receivedOn", SearchOperation.LESS_THAN, 
            simpleDateFormat.format(email2.getReceivedOn())));
        List<Email> result = emailRepository.findAll(Specification.where(spec));

        assertEquals(result.size(), 1);
        assertEquals(email1, result.get(0));
    }

    @Test
    public void shouldSearchWithMultipleAndCriteria() {
        createEmail("this is subject", "raw data", "from@address.domain", "friend@address.domain", "message-id", 1);
        createEmail("another project", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);
        var email = createEmail("an other sub", "raw data", "my@email.dot", "hello@address.domain", "message-id", 1);

        EmailSpecification spec = new EmailSpecification(new SpecSearchCriteria("subject", SearchOperation.STARTS_WITH, "an"));
        EmailSpecification spec2 = new EmailSpecification(new SpecSearchCriteria("fromAddress", SearchOperation.EQUALITY, "my@email.dot"));
        List<Email> result = emailRepository.findAll(Specification.where(spec).and(spec2));

        assertEquals(result.size(), 1);
        assertEquals(email, result.get(0));
    }

    @Test
    public void shouldSearchWithMultipleOrCriteria() {
        var email1 = createEmail("this is subject", "raw data", "from@address.domain", "friend@address.domain", "message-id", 1);
        var email2 = createEmail("another project", "raw data", "another-from@address.domain", "friend@address.domain", "message-id", 1);
        var email3 = createEmail("an other sub", "raw data", "my@email.dot", "hello@address.domain", "message-id", 1);

        EmailSpecification spec = new EmailSpecification(new SpecSearchCriteria("subject", SearchOperation.ENDS_WITH, "ect"));
        EmailSpecification spec2 = new EmailSpecification(new SpecSearchCriteria("fromAddress", SearchOperation.EQUALITY, "my@email.dot"));
        List<Email> result = emailRepository.findAll(Specification.where(spec).or(spec2));

        assertEquals(result.size(), 3);
        assertEquals(email1, result.get(0));
        assertEquals(email2, result.get(1));
        assertEquals(email3, result.get(2));
    }

    private Email createEmail(
        String subject,
        String rawData,
        String fromAddress,
        String toAdress, 
        String messageId,
        int minusMinutes
    ) {
        var localDateTime = LocalDateTime.now().minusMinutes(minusMinutes);
        var receivedOn = Date.from(localDateTime.atZone(ZoneOffset.systemDefault()).toInstant());

        var mail = new Email();
        mail.setSubject(subject);
        mail.setRawData(rawData);
        mail.setReceivedOn(receivedOn);
        mail.setFromAddress(fromAddress);
        mail.setToAddress(toAdress);
        mail.setMessageId(messageId);
        return emailRepository.save(mail);
    }
    
}
