package de.gessnerfl.fakesmtp.config;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Configuration
@Profile("develop")
public class DevelopDataInitializer {

    @Bean
    public CommandLineRunner initDevelopData(EmailRepository emailRepository, ResourceLoader resourceLoader) {
        return args -> {
            Email email1 = createEmail(
                    "welcome@company.com",
                    "john.doe@example.com",
                    "Willkommen bei unserem Service",
                    "Hallo John,\n\nvielen Dank für Ihre Registrierung. Wir freuen uns, Sie an Bord zu haben!\n\nMit freundlichen Grüßen,\nDas Team",
                    "welcome.txt",
                    loadAttachmentData(resourceLoader, "welcome.txt")
            );
            emailRepository.save(email1);

            Email email2 = createEmail(
                    "shop@onlineshop.de",
                    "max.mustermann@email.de",
                    "Bestellbestätigung #12345",
                    "Sehr geehrte(r) Kunde(in),\n\nIhre Bestellung wurde erfolgreich aufgenommen:\n\nArtikel: Premium Laptop\nPreis: 999,99 EUR\n\nVielen Dank für Ihren Einkauf!",
                    "rechnung.pdf",
                    loadAttachmentData(resourceLoader, "rechnung.pdf")
            );
            emailRepository.save(email2);

            Email email3 = createEmail(
                    "scheduler@firma.com",
                    "team@firma.com",
                    "Einladung: Wochenbesprechung am Freitag",
                    "Hallo Team,\n\nwir treffen uns am Freitag um 14:00 Uhr im Konferenzraum A.\n\nAgenda:\n1. Projektstatus\n2. Neue Features\n3. Q&A\n\nBitte bestätigen Sie Ihre Teilnahme.\n\nViele Grüße,\nMax",
                    "agenda.docx",
                    loadAttachmentData(resourceLoader, "agenda.docx")
            );
            emailRepository.save(email3);

            Email email4 = createEmail(
                    "noreply@security-bank.de",
                    "kunde@web.de",
                    "Passwort zurücksetzen",
                    "Hallo,\n\nSie haben angefordert, Ihr Passwort zurückzusetzen.\n\nKlicken Sie auf den folgenden Link:\nhttps://security-bank.de/reset?token=abc123\n\nFalls Sie dies nicht angefordert haben, ignorieren Sie diese E-Mail bitte.\n\nIhr Security-Bank Team",
                    "security-info.txt",
                    loadAttachmentData(resourceLoader, "welcome.txt")
            );
            emailRepository.save(email4);

            Email email5 = createEmail(
                    "newsletter@tech-news.com",
                    "abonnent@mail.de",
                    "Tech Weekly: Die neuesten Entwicklungen",
                    "Hallo Tech-Enthusiast,\n\nhier ist Ihre wöchentliche Zusammenfassung:\n\n• KI-Revolution in der Medizin\n• Neuer Blockchain-Standard veröffentlicht\n• Quantum Computing erreicht Meilenstein\n\nLesen Sie mehr auf unserer Website!\n\nIhr Tech-News Team",
                    "newsletter-attachments.zip",
                    loadAttachmentData(resourceLoader, "newsletter-attachments.zip")
            );
            emailRepository.save(email5);

            Email email6 = createEmail(
                    "support@helpdesk.io",
                    "frustrierter.user@gmail.com",
                    "RE: Support Ticket #9876 - Verbindungsprobleme",
                    "Hallo,\n\nvielen Dank für Ihre Anfrage. Wir haben Ihr Problem analysiert:\n\nDie Verbindungsabbrüche wurden durch ein Server-Update verursacht.\nDas Problem wurde behoben und sollte nicht mehr auftreten.\n\nBei weiteren Fragen stehen wir Ihnen gerne zur Verfügung.\n\nMit freundlichen Grüßen,\nDas Support-Team",
                    "support-diagram.png",
                    loadAttachmentData(resourceLoader, "support-diagram.png")
            );
            emailRepository.save(email6);

            Email email7 = createEmail(
                    "billing@dienstleister.de",
                    "kunde@firma-xyz.com",
                    "Zahlungserinnerung - Rechnung #2024-056",
                    "Sehr geehrte Damen und Herren,\n\nbitte überweisen Sie den offenen Betrag von 2.450,00 EUR bis zum 15.03.2024.\n\nRechnungsnummer: 2024-056\nFälligkeitsdatum: 15.03.2024\nIBAN: DE12 3456 7890 1234 5678 90\n\nBei Rückfragen erreichen Sie uns unter billing@dienstleister.de\n\nMit freundlichen Grüßen,\nDie Buchhaltung",
                    "rechnung-2024-056.pdf",
                    loadAttachmentData(resourceLoader, "rechnung.pdf")
            );
            emailRepository.save(email7);
        };
    }

    private Email createEmail(String from, String to, String subject, String body, String attachmentFilename, byte[] attachmentData) {
        Email email = new Email();
        email.setFromAddress(from);
        email.setToAddress(to);
        email.setSubject(subject);
        email.setReceivedOn(ZonedDateTime.now(ZoneId.of("UTC")));
        email.setRawData("From: " + from + "\nTo: " + to + "\nSubject: " + subject + "\n\n" + body);

        EmailContent content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData(body);
        email.addContent(content);

        EmailAttachment attachment = new EmailAttachment();
        attachment.setFilename(attachmentFilename);
        attachment.setData(attachmentData);
        email.addAttachment(attachment);

        return email;
    }

    private byte[] loadAttachmentData(ResourceLoader resourceLoader, String filename) {
        try {
            return resourceLoader.getResource("classpath:develop/attachments/" + filename).getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Could not load develop attachment resource '" + filename + "'", e);
        }
    }
}
