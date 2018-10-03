package de.gessnerfl.fakesmtp;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class TestDataCreator {

    public static final int NUMBER_OF_TEST_EMAILS = 5;

    public static void main(String[] args) {
        for(int i = 0; i < NUMBER_OF_TEST_EMAILS; i++){
            createEmail(i);
            createHtmlEmail(i);
            createMimeAlternativeEmail(i);
        }
    }

    private static void createEmail(int i) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("receiver@exmaple.com");
        message.setFrom("sender@example.com");
        message.setSubject("Test-Plain-Mail " + i);
        message.setText("This is the test mail number "+i);
        getEmailSender().send(message);
    }

    private static void createHtmlEmail(int i){
        try {
            JavaMailSender sender = getEmailSender();

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setTo("receiver@exmaple.com");
            helper.setFrom("sender@example.com");
            helper.setSubject("Test-Html-Mail " + i);
            helper.setText("<html><head></head><body>This is the test mail number " + i + "</body>", true);

            sender.send(message);
        } catch (MessagingException e){
            throw new RuntimeException("Failed to create mail", e);
        }
    }

    private static void createMimeAlternativeEmail(int i) {
        try {
            JavaMailSender sender = getEmailSender();

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo("receiver@exmaple.com");
            helper.setFrom("sender@example.com");
            helper.setSubject("Test-Alternative-Mail " + i);
            helper.setText("This is the test mail number" + i, "<html><head></head><body>This is the test mail number " + i + "</body>");
            helper.addAttachment("app-icon.png", new ClassPathResource("/static/gfx/app-icon.png"));
            helper.addAttachment("customizing.css", new ClassPathResource("/static/customizing.css"));
            sender.send(message);
        } catch (MessagingException e){
            throw new RuntimeException("Failed to create mail", e);
        }
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
