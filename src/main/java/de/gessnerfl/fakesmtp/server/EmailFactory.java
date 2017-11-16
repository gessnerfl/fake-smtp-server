package de.gessnerfl.fakesmtp.server;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
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

    public Email convert(String from, String to, InputStream data) throws IOException {
        String rawData = convertStreamToString(data);
        try {
            Session s = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(s, new ByteArrayInputStream(rawData.getBytes(StandardCharsets.UTF_8)));
            String subject = Objects.toString(mimeMessage.getSubject(), UNDEFINED);
            ContentType contentType = ContentType.fromString(mimeMessage.getContentType());
            Object messageContent = mimeMessage.getContent();
            switch (contentType) {
                case HTML:
                case PLAIN: {
                    return new Email.Builder()
                            .fromAddress(from)
                            .toAddress(to)
                            .receivedOn(timestampProvider.now())
                            .subject(subject)
                            .rawData(rawData)
                            .content(Objects.toString(messageContent, rawData).trim())
                            .contentType(contentType)
                            .build();
                }
                case MULTIPART_ALTERNATIVE: {
                    Multipart multipart = (Multipart) messageContent;
                    Email email = null;
                    for (int i = 0; i < multipart.getCount(); i++) {
                        BodyPart part = multipart.getBodyPart(i);
                        ContentType partContentType = ContentType.fromString(part.getContentType());
                        Object partContent = part.getContent();
                        email = new Email.Builder()
                                .fromAddress(from)
                                .toAddress(to)
                                .receivedOn(timestampProvider.now())
                                .subject(subject)
                                .rawData(rawData)
                                .content(Objects.toString(partContent, rawData).trim())
                                .contentType(partContentType)
                                .build();
                        if (partContentType == ContentType.HTML) break;
                    }
                    return email;
                }
                default:
                    throw new IllegalStateException("Unsupported e-mail content type " + contentType.name());
            }
        } catch (MessagingException e) {
            data.reset();
            return new Email.Builder()
                    .fromAddress(from)
                    .toAddress(to)
                    .receivedOn(timestampProvider.now())
                    .subject(UNDEFINED)
                    .rawData(rawData)
                    .content(rawData)
                    .contentType(ContentType.PLAIN)
                    .build();
        }
    }

    private String convertStreamToString(InputStream data) throws IOException {

        return IOUtils.toString(data, StandardCharsets.UTF_8);
    }
}
