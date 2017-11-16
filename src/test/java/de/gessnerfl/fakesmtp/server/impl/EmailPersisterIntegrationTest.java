package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.TestResourceUtil;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

@ActiveProfiles("integrationtest")
@RunWith(SpringRunner.class)
@SpringBootTest
public class EmailPersisterIntegrationTest {
    private static final String SENDER = "sender";
    private static final String RECEIVER = "receiver";

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailPersister sut;

    @Before
    public void setup(){
        emailRepository.deleteAll();
    }

    @Test
    public void shouldCreateEmailForEmlFileWithSubject() throws Exception {
        String testFilename = "mail-with-subject.eml";
        InputStream data = TestResourceUtil.getTestFile(testFilename);
        String rawData = TestResourceUtil.getTestFileContent(testFilename);

        sut.deliver(SENDER, RECEIVER, data);

        List<Email> mails = emailRepository.findAll();
        assertThat(mails, hasSize(1));

        Email mail = mails.get(0);

        assertNotNull(mail.getId());
        assertEquals(SENDER, mail.getFromAddress());
        assertEquals(RECEIVER, mail.getToAddress());
        assertEquals("This is the mail title", mail.getSubject());
        assertEquals(rawData, mail.getRawData());
        assertEquals("This is the message content", mail.getContent());
        assertNotNull(mail.getReceivedOn());
    }

    @Test
    public void shouldCreateEmailForEmlFileWithoutSubject() throws Exception {
        String testFilename = "mail-without-subject.eml";
        InputStream data = TestResourceUtil.getTestFile(testFilename);
        String rawData = TestResourceUtil.getTestFileContent(testFilename);

        sut.deliver(SENDER, RECEIVER, data);

        List<Email> mails = emailRepository.findAll();
        assertThat(mails, hasSize(1));

        Email mail = mails.get(0);

        assertNotNull(mail.getId());
        assertEquals(SENDER, mail.getFromAddress());
        assertEquals(RECEIVER, mail.getToAddress());
        assertEquals(EmailFactory.UNDEFINED, mail.getSubject());
        assertEquals(rawData, mail.getRawData());
        assertEquals("This is the message content", mail.getContent());
        assertNotNull(mail.getReceivedOn());
    }

    @Test
    public void shouldCreateMailForPlainText() throws Exception {
        String rawData = "this is just some dummy content";
        InputStream data = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.UTF_8));

        sut.deliver(SENDER, RECEIVER, data);

        List<Email> mails = emailRepository.findAll();
        assertThat(mails, hasSize(1));

        Email mail = mails.get(0);

        assertNotNull(mail.getId());
        assertEquals(SENDER, mail.getFromAddress());
        assertEquals(RECEIVER, mail.getToAddress());
        assertEquals(EmailFactory.UNDEFINED, mail.getSubject());
        assertEquals(rawData, mail.getRawData());
        assertEquals(rawData, mail.getContent());
        assertNotNull(mail.getReceivedOn());
    }
}