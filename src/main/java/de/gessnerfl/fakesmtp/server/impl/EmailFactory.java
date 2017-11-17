package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.Objects;
import java.util.Properties;

@Service
public class EmailFactory {
    public static final String UNDEFINED = "<undefined>";

    private final TimestampProvider timestampProvider;

    @Autowired
    public EmailFactory(TimestampProvider timestampProvider) {
        this.timestampProvider = timestampProvider;
    }

    public Email convert(RawData rawData) throws IOException {
        try {
            Session s = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(s, rawData.getContentAsStream());
            String subject = Objects.toString(mimeMessage.getSubject(), UNDEFINED);
            ContentType contentType = ContentType.fromString(mimeMessage.getContentType());
            Object messageContent = mimeMessage.getContent();

            switch (contentType) {
                case HTML:
                case PLAIN:
                    return buildPlainOrHtmlEmail(rawData, subject, contentType, messageContent);
                case MULTIPART_ALTERNATIVE:
                    return buildMultipartAlternativeMail(rawData, subject, (Multipart) messageContent);
                default:
                    throw new IllegalStateException("Unsupported e-mail content type " + contentType.name());
            }
        } catch (MessagingException e) {
            return buildFallbackEmail(rawData);
        }
    }

    private Email buildPlainOrHtmlEmail(RawData rawData, String subject, ContentType contentType, Object messageContent) {
        return new Email.Builder()
                .fromAddress(rawData.getFrom())
                .toAddress(rawData.getTo())
                .receivedOn(timestampProvider.now())
                .subject(subject)
                .rawData(rawData.getContentAsString())
                .content(Objects.toString(messageContent, rawData.getContentAsString()).trim())
                .contentType(contentType)
                .build();
    }

    private Email buildMultipartAlternativeMail(RawData rawData, String subject, Multipart multipart) throws MessagingException, IOException {
        Email email = null;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            ContentType partContentType = ContentType.fromString(part.getContentType());
            Object partContent = part.getContent();
            email = new Email.Builder()
                    .fromAddress(rawData.getFrom())
                    .toAddress(rawData.getTo())
                    .receivedOn(timestampProvider.now())
                    .subject(subject)
                    .rawData(rawData.getContentAsString())
                    .content(Objects.toString(partContent, rawData.getContentAsString()).trim())
                    .contentType(partContentType)
                    .build();
            if (partContentType == ContentType.HTML) break;
        }
        return email;
    }

    private Email buildFallbackEmail(RawData rawData) {
        return new Email.Builder()
                .fromAddress(rawData.getFrom())
                .toAddress(rawData.getTo())
                .receivedOn(timestampProvider.now())
                .subject(UNDEFINED)
                .rawData(rawData.getContentAsString())
                .content(rawData.getContentAsString())
                .contentType(ContentType.PLAIN)
                .build();
    }

}
