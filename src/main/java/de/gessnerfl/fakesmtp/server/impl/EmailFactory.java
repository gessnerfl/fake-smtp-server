package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
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
                    return createPlainOrHtmlMail(rawData, subject, contentType, messageContent);
                case MULTIPART_ALTERNATIVE:
                case MULTIPART_MIXED:
                case MULTIPART_RELATED:
                    return createMultipartMail(rawData, subject, (Multipart) messageContent);
                default:
                    throw new IllegalStateException("Unsupported e-mail content type " + contentType.name());
            }
        } catch (MessagingException e) {
            return buildFallbackEmail(rawData);
        }
    }

    private Email createPlainOrHtmlMail(RawData rawData, String subject, ContentType contentType, Object messageContent) {
        Email email = createEmailFromRawData(rawData);
        email.setSubject(subject);
        createEmailContent(rawData, contentType, messageContent).ifPresent(email::addContent);
        return email;
    }

    private Email createMultipartMail(RawData rawData, String subject, Multipart multipart) throws MessagingException, IOException {
        Email email = createEmailFromRawData(rawData);
        email.setSubject(subject);

        appendMultipartBodyParts(email, rawData, multipart);

        return email;
    }

    private void appendMultipartBodyParts(Email email, RawData rawData, Multipart multipart) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            final BodyPart part = multipart.getBodyPart(i);
            final String disposition = part.getDisposition();
            if (disposition == null || disposition.equalsIgnoreCase(Part.INLINE)) {
                appendMultipartContent(email, rawData, part);
            } else if (disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                EmailAttachment attachment = createAttachment(part);
                email.addAttachment(attachment);
            }
        }
    }

    private void appendMultipartContent(Email email, RawData rawData, BodyPart part) throws MessagingException, IOException {
        ContentType partContentType = ContentType.fromString(part.getContentType());
        if (partContentType == ContentType.HTML || partContentType == ContentType.PLAIN) {
            final Object partContent = part.getContent();
            createEmailContent(rawData, partContentType, partContent).ifPresent(email::addContent);
        }else if(partContentType == ContentType.MULTIPART_RELATED || partContentType == ContentType.MULTIPART_ALTERNATIVE){
            final Multipart content = (Multipart)part.getContent();
            appendMultipartBodyParts(email, rawData, content);
        }
    }

    private Email buildFallbackEmail(RawData rawData) {
        EmailContent content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData(rawData.getContentAsString());

        Email email = createEmailFromRawData(rawData);
        email.setSubject(UNDEFINED);
        email.addContent(content);
        return email;
    }

    private Email createEmailFromRawData(RawData rawData) {
        Email email = new Email();
        email.setFromAddress(rawData.getFrom());
        email.setToAddress(rawData.getTo());
        email.setReceivedOn(timestampProvider.now());
        email.setRawData(rawData.getContentAsString());
        return email;
    }

    private Optional<EmailContent> createEmailContent(RawData rawData, ContentType contentType, Object messageContent) {
        String data = Optional.ofNullable(Objects.toString(messageContent, null))
                .map(this::normalizeContent).orElseGet(() -> normalizeContent(rawData.getContentAsString()));
        if(data == null){
            return Optional.empty();
        }
        EmailContent content = new EmailContent();
        content.setContentType(contentType);
        content.setData(data);
        return Optional.of(content);
    }

    private EmailAttachment createAttachment(BodyPart part) throws MessagingException, IOException {
        EmailAttachment attachment = new EmailAttachment();
        attachment.setFilename(part.getFileName());
        attachment.setData(IOUtils.toByteArray(part.getInputStream()));
        return attachment;
    }

    private String normalizeContent(String input){
        return input != null && !input.trim().isEmpty() ? input.trim() : null;
    }
}
