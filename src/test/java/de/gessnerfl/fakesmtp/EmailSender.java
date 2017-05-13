package de.gessnerfl.fakesmtp;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

public class EmailSender {

    public static void main(String[] args) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("receiver@exmaple.com");
        message.setFrom("sender@example.com");
        message.setSubject("Test-Mail");
        message.setText("This is a test mail");
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
