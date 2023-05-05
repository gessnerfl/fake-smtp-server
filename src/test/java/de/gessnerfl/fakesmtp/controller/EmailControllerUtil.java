package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

class EmailControllerUtil {

    private EmailControllerUtil() { }

    public static Email prepareRandomEmail(int minusMinutes) {
        var randomToken = RandomStringUtils.randomAlphanumeric(6);
        var localDateTime = LocalDateTime.now().minusMinutes(minusMinutes);
        var receivedOn = Date.from(localDateTime.atZone(ZoneOffset.systemDefault()).toInstant());

        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content " + randomToken);

        var attachment = new EmailAttachment();
        attachment.setFilename("test.txt");
        attachment.setData("This is some test data".getBytes(StandardCharsets.UTF_8));
        
        return prepareEmail(attachment, content, "Test Subject " + randomToken, "Test Content " + randomToken, receivedOn,
            "sender@example.com", "receiver@example.com", "<message-id>", minusMinutes);
    }

    public static Email prepareEmail(String subject, String toAdress, int minusMinutes) {
        var randomToken = RandomStringUtils.randomAlphanumeric(6);
        var localDateTime = LocalDateTime.now().minusMinutes(minusMinutes);
        var receivedOn = Date.from(localDateTime.atZone(ZoneOffset.systemDefault()).toInstant());

        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content " + randomToken);

        var attachment = new EmailAttachment();
        attachment.setFilename("test.txt");
        attachment.setData("This is some test data".getBytes(StandardCharsets.UTF_8));

        return prepareEmail(attachment, content, subject, "Test Content " + randomToken, 
            receivedOn, "sender@example.com", toAdress, "<message-id>", minusMinutes);
    }

    public static Email prepareEmail(
        EmailAttachment emailAttachment,
        EmailContent emailContent, 
        String subject,
        String rawData,
        Date receivedOn,
        String fromAddress,
        String toAdress, 
        String messageId,
        int minusMinutes
    ) {
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