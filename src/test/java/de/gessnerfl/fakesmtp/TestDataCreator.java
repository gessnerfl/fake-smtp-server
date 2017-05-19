package de.gessnerfl.fakesmtp;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

public class TestDataCreator {

    public static final int NUMBER_OF_TEST_EMAILS = 45;

    public static void main(String[] args) {
        for(int i = 0; i < NUMBER_OF_TEST_EMAILS; i++){
            createEmail(i);
        }
    }

    private static void createEmail(int i) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("receiver@exmaple.com");
        message.setFrom("sender@example.com");
        message.setSubject("Test-Mail " + i);
        message.setText("This is the test mail number "+i);
        getEmailSender().send(message);
    }

    private static JavaMailSender getEmailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(5025);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "false");

        return mailSender;
    }

}
