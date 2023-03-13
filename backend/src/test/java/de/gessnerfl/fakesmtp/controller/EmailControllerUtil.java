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

        var mail = new Email();
        mail.setSubject("Test Subject " + randomToken);
        mail.setRawData("Test Content " + randomToken);
        mail.setReceivedOn(receivedOn);
        mail.setFromAddress("sender@example.com");
        mail.setToAddress("receiver@example.com");
        mail.addContent(content);
        mail.addAttachment(attachment);
        return mail;
    }

}