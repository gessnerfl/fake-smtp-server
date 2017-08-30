package de.gessnerfl.fakesmtp.server;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
            ContentType contentType = mimeMessage.getContentType().startsWith("text/html") ? ContentType.HTML : ContentType.PLAIN;
            String subject = mimeMessage.getSubject() != null ? mimeMessage.getSubject() : UNDEFINED;
            String content = extractContent(mimeMessage.getContent(), rawData);
            return createEmail(from, to, subject, rawData, content, contentType);
        } catch (MessagingException e) {
            data.reset();
            return createEmail(from, to, UNDEFINED, rawData, rawData, ContentType.PLAIN);
        }
    }

    private String extractContent(Object content, String rawData) throws IOException, MessagingException {
        String data = content != null ? content.toString().trim() : null;
        return StringUtils.isEmpty(data) ? rawData : data;
    }

    private Email createEmail(String from, String to, String subject, String rawData, String content, ContentType contentType){
        Email email = new Email();
        email.setFromAddress(from);
        email.setToAddress(to);
        email.setReceivedOn(timestampProvider.now());
        email.setSubject(subject);
        email.setRawData(rawData);
        email.setContent(content);
        email.setContentType(contentType);
        return email;
    }

    private String convertStreamToString(InputStream data) throws IOException {

        return IOUtils.toString(data, StandardCharsets.UTF_8);
    }
}
