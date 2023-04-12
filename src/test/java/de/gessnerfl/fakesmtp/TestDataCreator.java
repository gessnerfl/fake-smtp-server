package de.gessnerfl.fakesmtp;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;

public class TestDataCreator {

    private static final int NUMBER_OF_TEST_EMAILS = 5;

    public static void main(String[] args) {
        for(var i = 0; i < NUMBER_OF_TEST_EMAILS; i++){
            createEmail(i);
            createHtmlEmail(i);
            createMimeAlternativeEmail(i);
        }
        createLongPlainEmail();
        createHtmlEmailWithoutHtmlStructure();
    }

    private static void createEmail(int i) {
        var message = new SimpleMailMessage();
        message.setTo("receiver@example.com");
        message.setFrom("sender@example.com");
        message.setSubject("Test-Plain-Mail " + i);
        message.setText("This is the test mail number "+i);
        getEmailSender().send(message);
    }

    private static void createLongPlainEmail() {
        var message = new SimpleMailMessage();
        message.setTo("receiver@example.com");
        message.setFrom("sender@example.com");
        message.setSubject("Long-Plain-Mail ");
        message.setText("Das grundsätzliche Problem liegt laut Dolan-Gavitt darin, dass die sogenannten subnormalen Zahlen – die auch als nichtnormalisierte Fließkommazahlen bezeichnet werden – durch die Fehler als Null-Wert behandelt werden. Diese Zahlen besitzen eine 0 vor dem Binärpunkt, während bei den “normalen“ Fließkommazahlen an dieser Stelle immer eine 1 zu finden ist. Die subnormalen Zahlen bieten die Garantie, dass bei Addition und Subtraktion von Gleitkommazahlen niemals ein Unterlauf (underflow) zweier nahe beieinander liegende Gleitkommazahlen auftreten kann. Sie haben immer eine darstellbare Differenz ungleich Null.\n" +
                "\n" +
                "Durch eine Suche fand der Wissenschaftler einen entsprechenden GitHub-Eintrag, der auf eine gemeinsam genutzte Bibliothek hinwies, die mit der gcc/clang-Option -ffast-math kompiliert und als Verursacher des Problems geladen wurde. Dabei stellte sich heraus, dass der Compiler bei Aktivierung dieser Option sogar bei gemeinsam genutzten Bibliotheken einen Konstruktor einbindet, der die FTZ/DAZ-Flags setzt, sobald die Bibliothek geladen wird.\n" +
                "\n" +
                "Das bedeutet, dass jede Anwendung, die diese Bibliothek lädt, ihr Fließkommaverhalten für den gesamten Prozess ändert. Zudem aktiviert die Option -Ofast, die auf den ersten Blick nach einem \"Mach mein Programm schnell\"-Flag klingt, automatisch -ffast-math, sodass einige Projekte es unwissentlich aktivieren, ohne dass ein Entwickler oder eine Entwicklerin sich der Auswirkungen bewusst ist.");
        getEmailSender().send(message);
    }

    private static void createHtmlEmail(int i){
        try {
            var sender = getEmailSender();

            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message);
            helper.setTo("receiver@example.com");
            helper.setFrom("sender@example.com");
            helper.setSubject("Test-Html-Mail " + i);
            helper.setText("<html><head><style>body {color: green; font-size:30px;}</style></head><body>This is the test mail number " + i + "</body>", true);

            sender.send(message);
        } catch (MessagingException e){
            throw new RuntimeException("Failed to create mail", e);
        }
    }

    private static void createHtmlEmailWithoutHtmlStructure(){
        try {
            var sender = getEmailSender();

            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message);
            helper.setTo("receiver@example.com");
            helper.setFrom("sender@example.com");
            helper.setSubject("Test-Html-Mail-Format-Only");
            helper.setText("<p>This is the test mail <b>With formatting only</b></p><p>foo bar</p>", true);

            sender.send(message);
        } catch (MessagingException e){
            throw new RuntimeException("Failed to create mail", e);
        }
    }

    private static void createMimeAlternativeEmail(int i) {
        try {
            var sender = getEmailSender();

            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);
            helper.setTo("receiver@example.com");
            helper.setFrom("sender@example.com");
            helper.setSubject("Test-Alternative-Mail " + i);
            helper.setText("This is the test mail number" + i, "<html><head></head><body>This is the test mail number " + i + "<img src=\"cid:icon1\"></img><img src=\"cid:icon2\"></img></body>");
            helper.addInline("icon1", new ClassPathResource("/static/gfx/app-icon.png"));
            helper.addInline("icon2", new ClassPathResource("/static/gfx/inbox-solid.png"));
            helper.addAttachment("app-icon.png", new ClassPathResource("/static/gfx/app-icon.png"));
            helper.addAttachment("inbox-solid.png", new ClassPathResource("/static/gfx/inbox-solid.png"));
            sender.send(message);
        } catch (MessagingException e){
            throw new RuntimeException("Failed to create mail", e);
        }
    }

    private static JavaMailSender getEmailSender() {
        var mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(5025);

        var props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.debug", "false");

        return mailSender;
    }

}
