package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;

class EmailControllerUtil {

    private EmailControllerUtil() { }

    public static Email prepareRandomEmail(int minusMinutes) {
        var randomToken = RandomStringUtils.randomAlphanumeric(6);
        var receivedOn = getUtcNow().minusMinutes(minusMinutes);

        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content " + randomToken);

        var attachment = new EmailAttachment();
        attachment.setFilename("test.txt");
        attachment.setData("This is some test data".getBytes(StandardCharsets.UTF_8));
        
        return prepareEmail(attachment, content, "Test Subject " + randomToken, "Test Content " + randomToken, receivedOn,
            "sender@example.com", "receiver@example.com", "<message-id>");
    }

    public static Email prepareEmail(String subject, String toAdress, int minusMinutes) {
        var randomToken = RandomStringUtils.randomAlphanumeric(6);
        var receivedOn = getUtcNow().minusMinutes(minusMinutes);

        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content " + randomToken);

        var attachment = new EmailAttachment();
        attachment.setFilename("test.txt");
        attachment.setData("This is some test data".getBytes(StandardCharsets.UTF_8));

        return prepareEmail(attachment, content, subject, "Test Content " + randomToken, 
            receivedOn, "sender@example.com", toAdress, "<message-id>");
    }

    public static Email prepareEmail(String subject, String toAdress, int minusMinutes, String messageId) {
        var randomToken = RandomStringUtils.randomAlphanumeric(6);
        var receivedOn = getUtcNow().minusMinutes(minusMinutes);

        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content " + randomToken);

        var attachment = new EmailAttachment();
        attachment.setFilename("test.txt");
        attachment.setData("This is some test data".getBytes(StandardCharsets.UTF_8));

        return prepareEmail(attachment, content, subject, "Test Content " + randomToken, 
            receivedOn, "sender@example.com", toAdress, messageId);
    }

    private static ZonedDateTime getUtcNow() {
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }

    public static Email prepareEmail(
        EmailAttachment emailAttachment,
        EmailContent emailContent, 
        String subject,
        String rawData,
        ZonedDateTime receivedOn,
        String fromAddress,
        String toAdress, 
        String messageId) {
        var mail = new Email();
        mail.setSubject(subject);
        mail.setRawData(rawData);
        mail.setReceivedOn(receivedOn);
        mail.setFromAddress(fromAddress);
        mail.setToAddress(toAdress);
        mail.setMessageId(messageId);
        mail.addContent(emailContent);
        mail.addAttachment(emailAttachment);
        return mail;
    }

}