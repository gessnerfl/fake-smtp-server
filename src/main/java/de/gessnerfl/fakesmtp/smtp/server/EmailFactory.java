package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import de.gessnerfl.fakesmtp.model.*;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import java.io.InputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Service
public class EmailFactory {
    public static final String UNDEFINED = "<undefined>";
    private static final Logger logger = LoggerFactory.getLogger(EmailFactory.class);

    private final TimestampProvider timestampProvider;
    private final FakeSmtpConfigurationProperties configurationProperties;

    @Autowired
    public EmailFactory(TimestampProvider timestampProvider, FakeSmtpConfigurationProperties configurationProperties) {
        this.timestampProvider = timestampProvider;
        this.configurationProperties = configurationProperties;
    }

    public Email convert(RawData rawData) throws IOException {
        try {
            var mimeMessage = rawData.toMimeMessage();
            var subject = Objects.toString(mimeMessage.getSubject(), UNDEFINED);
            var contentType = ContentType.fromString(mimeMessage.getContentType());
            var messageContent = mimeMessage.getContent();
            var messageId = mimeMessage.getMessageID();

            return switch (contentType) {
                case HTML, PLAIN, OCTET_STREAM ->
                        createPlainOrHtmlMail(rawData, subject, contentType, messageContent, messageId);
                case MULTIPART_ALTERNATIVE, MULTIPART_MIXED, MULTIPART_RELATED ->
                        createMultipartMail(rawData, subject, (Multipart) messageContent, messageId);
                default ->
                        throw new IllegalStateException("Unsupported e-mail content type " + mimeMessage.getContentType());
            };
        } catch (MessagingException e) {
            return buildFallbackEmail(rawData);
        }
    }

    private Email createPlainOrHtmlMail(RawData rawData, String subject, ContentType contentType, Object messageContent, String messageId) {
        var email = createEmailFromRawData(rawData);
        email.setSubject(subject);
        createEmailContent(rawData, contentType, messageContent).ifPresent(email::addContent);
        email.setMessageId(messageId);
        return email;
    }

    private Email createMultipartMail(RawData rawData, String subject, Multipart multipart, String messageId) throws MessagingException, IOException {
        var email = createEmailFromRawData(rawData);
        email.setSubject(subject);
        email.setMessageId(messageId);

        appendMultipartBodyParts(email, rawData, multipart);

        return email;
    }

    private void appendMultipartBodyParts(Email email, RawData rawData, Multipart multipart) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            final var part = multipart.getBodyPart(i);
            final var disposition = part.getDisposition();
            if (disposition == null || disposition.equalsIgnoreCase(Part.INLINE)) {
                appendMultipartContent(email, rawData, part);
            } else if (disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                EmailAttachment attachment;
                try {
                    attachment = createAttachment(part);
                } catch (EmailPartTooLargeException e) {
                    attachment = createSkippedAttachment(part, e.getMessage());
                    logger.warn(e.getMessage());
                }
                email.addAttachment(attachment);
            }
        }
    }

    private void appendMultipartContent(Email email, RawData rawData, BodyPart part) throws MessagingException, IOException {
        var partContentType = ContentType.fromString(part.getContentType());
        if (partContentType == ContentType.HTML || partContentType == ContentType.PLAIN) {
            final var partContent = part.getContent();
            createEmailContent(rawData, partContentType, partContent).ifPresent(email::addContent);
        } else if (partContentType == ContentType.MULTIPART_RELATED || partContentType == ContentType.MULTIPART_ALTERNATIVE) {
            final var content = (Multipart) part.getContent();
            appendMultipartBodyParts(email, rawData, content);
        } else if (partContentType == ContentType.IMAGE) {
            try {
                createInlineImage(part).ifPresent(email::addInlineImage);
            } catch (EmailPartTooLargeException e) {
                createSkippedInlineImage(part, e.getMessage()).ifPresent(email::addInlineImage);
                logger.warn(e.getMessage());
            }
        }
    }

    private Email buildFallbackEmail(RawData rawData) {
        var content = new EmailContent();
        content.setContentType(ContentType.PLAIN);
        content.setData(rawData.getContentAsString());

        var email = createEmailFromRawData(rawData);
        email.setSubject(UNDEFINED);
        email.addContent(content);
        return email;
    }

    private Email createEmailFromRawData(RawData rawData) {
        var email = new Email();
        email.setFromAddress(rawData.getFrom());
        email.setToAddress(rawData.getTo());
        email.setReceivedOn(timestampProvider.now());
        email.setRawData(rawData.getContentAsString());
        return email;
    }

    private Optional<EmailContent> createEmailContent(RawData rawData, ContentType contentType, Object messageContent) {
        var data = getMessageContentAsString(messageContent, false)
                .map(this::normalizeContent)
                .orElseGet(() -> normalizeContent(rawData.getContentAsString()));
        if (data == null) {
            return Optional.empty();
        }
        var content = new EmailContent();
        content.setContentType(contentType);
        content.setData(data);
        return Optional.of(content);
    }

    private Optional<InlineImage> createInlineImage(final BodyPart part) throws MessagingException, IOException {
        var contentType = part.getContentType();
        Object rawContent = part.getContent();
        Optional<String> data = getMessageContentAsString(rawContent, true);
        return extractContentId(part).flatMap(contentId ->
            data.map(d -> {
                var img = new InlineImage();
                img.setContentId(contentId);
                img.setContentType(contentType);
                img.setData(d);
                return img;
            }));
    }

    private Optional<String> getMessageContentAsString(Object rawContent, boolean applyAttachmentLimit) {
        var content = rawContent instanceof java.io.InputStream stream
                ? readStreamToBase64(stream, applyAttachmentLimit)
                : Objects.toString(rawContent, null);
        return Optional.ofNullable(content);
    }

    private String readStreamToBase64(java.io.InputStream stream, boolean applyAttachmentLimit) {
        try {
            var byteArray = applyAttachmentLimit ? readBinaryDataWithLimit(stream, "Inline image") : stream.readAllBytes();
            return Base64.getEncoder().encodeToString(byteArray);
        } catch (IOException e) {
            throw new EmailProcessingException("Failed to read message content", e);
        }
    }

    private Optional<String> extractContentId(BodyPart part) throws MessagingException {
        var headerValues = part.getHeader("Content-ID");
        if (headerValues == null || headerValues.length == 0) {
            return Optional.empty();
        }

        return normalizeContentIdHeaderValue(headerValues[0]);
    }

    private Optional<String> normalizeContentIdHeaderValue(String rawHeaderValue) {
        var normalizedHeaderValue = normalizeContent(rawHeaderValue);
        if (normalizedHeaderValue == null) {
            return Optional.empty();
        }

        if (isWrappedInAngleBrackets(normalizedHeaderValue)) {
            var unwrappedValue = normalizeContent(normalizedHeaderValue.substring(1, normalizedHeaderValue.length() - 1));
            return Optional.ofNullable(unwrappedValue);
        }

        if (normalizedHeaderValue.startsWith("<") || normalizedHeaderValue.endsWith(">")) {
            logger.debug("Malformed Content-ID header value '{}'. Using trimmed raw value instead.", normalizedHeaderValue);
        }

        return Optional.of(normalizedHeaderValue);
    }

    private boolean isWrappedInAngleBrackets(String value) {
        return value.length() >= 2 && value.startsWith("<") && value.endsWith(">");
    }

    private EmailAttachment createAttachment(BodyPart part) throws MessagingException, IOException {
        var attachment = new EmailAttachment();
        var filename = Objects.toString(part.getFileName(), "<unnamed-attachment>");
        attachment.setFilename(filename);
        attachment.setData(readBinaryDataWithLimit(part.getInputStream(), "Attachment '" + filename + "'"));
        return attachment;
    }

    private EmailAttachment createSkippedAttachment(BodyPart part, String message) throws MessagingException {
        var attachment = new EmailAttachment();
        attachment.setFilename(Objects.toString(part.getFileName(), "<unnamed-attachment>"));
        attachment.setData(new byte[0]);
        attachment.setProcessingStatus(EmailPartProcessingStatus.SKIPPED_TOO_LARGE);
        attachment.setProcessingMessage(message);
        return attachment;
    }

    private Optional<InlineImage> createSkippedInlineImage(BodyPart part, String message) throws MessagingException {
        var contentType = Objects.toString(part.getContentType(), "image/*");
        return extractContentId(part).map(contentId -> {
            var image = new InlineImage();
            image.setContentId(contentId);
            image.setContentType(contentType);
            image.setData("");
            image.setProcessingStatus(EmailPartProcessingStatus.SKIPPED_TOO_LARGE);
            image.setProcessingMessage(message);
            return image;
        });
    }

    private byte[] readBinaryDataWithLimit(InputStream stream, String partDescription) throws IOException {
        var maxSize = configurationProperties.getMaxAttachmentSize().toBytes();
        var maxReadableBytes = maxSize + 1;
        
        try (var limitedStream = BoundedInputStream.builder()
                .setInputStream(stream)
                .setMaxCount(maxReadableBytes)
                .get()) {
            var byteArray = limitedStream.readAllBytes();
            if (byteArray.length > maxSize) {
                throw new EmailPartTooLargeException(buildSkippedTooLargeMessage(partDescription, maxSize));
            }
            return byteArray;
        }
    }

    private String buildSkippedTooLargeMessage(String partDescription, long maxSizeBytes) {
        return "SKIPPED_TOO_LARGE: " + partDescription + " exceeded configured max attachment size of "
                + DataSize.ofBytes(maxSizeBytes);
    }

    private String normalizeContent(String input) {
        return input != null && !input.trim().isEmpty() ? input.trim() : null;
    }
}
