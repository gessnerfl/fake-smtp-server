package de.gessnerfl.fakesmtp;

import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.smtp.server.SmtpServer;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext
@Transactional
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles({"integrationtest_with_tls_required", "integrationtest", "default"})
public class TLSIntegrationTest {

    @Autowired
    private EmailRepository emailRepository;
    @Autowired
    private SmtpServer smtpServer;

    @Test
    void shouldSendMailViaTLS() {
        var mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(smtpServer.getPort());

        var props = mailSender.getJavaMailProperties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.auth", "false");
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
        props.setProperty("mail.smtp.ssl.trust", "*");
        props.setProperty("mail.debug", "false");

        var uniqueRandomName = "Test-Mail-" + RandomStringUtils.randomAlphanumeric(24);
        var message = new SimpleMailMessage();
        message.setTo("receiver@example.com");
        message.setFrom("sender@example.com");
        message.setSubject(uniqueRandomName);
        message.setText("This is the test mail");
        mailSender.send(message);

        var mails = emailRepository.findBySubject(uniqueRandomName);
        assertThat(mails, hasSize(1));
        assertEquals(uniqueRandomName, mails.get(0).getSubject());
    }
}
