package de.gessnerfl.fakesmtp;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class EmailBuilder {
    private String randomToken;
    private String to;
    private String from;
    private String subject;
    private String rawData;
    private int minutesAgo;

    public EmailBuilder() {
        this.randomToken = RandomStringUtils.randomAlphanumeric(6);
        this.subject = "Test Subject " + this.randomToken;
        this.from = "sender@example.com";
        this.to = "receiver@example.com";
        this.rawData = "Test Content " + this.randomToken;
    }

    public EmailBuilder from(String fromAddress) {
        this.from = fromAddress;
        return this;
    }

    public EmailBuilder to(String toAddress) {
        this.to = toAddress;
        return this;
    }

    public EmailBuilder withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public EmailBuilder withRawData(String rawData) {
        this.rawData = rawData;
        return this;
    }

    public EmailBuilder receivedMinutesAgo(int minutesAgo) {
        this.minutesAgo = minutesAgo;
        return this;
    }

    public Email build() {
        var localDateTime = LocalDateTime.now().minusMinutes(this.minutesAgo);
        var receivedOn = Date.from(localDateTime.atZone(ZoneOffset.systemDefault()).toInstant());

        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData("Test Content " + this.randomToken);

        var attachment = new EmailAttachment();
        attachment.setFilename("test.txt");
        attachment.setData("This is some test data".getBytes(StandardCharsets.UTF_8));

        var email = new Email();
        email.setSubject(this.subject);
        email.setRawData(this.rawData);
        email.setReceivedOn(receivedOn);
        email.setFromAddress(this.from);
        email.setToAddress(this.to);
        email.addContent(content);
        email.addAttachment(attachment);
        
        return email;
    }

}