package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.TestResourceUtil;
import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailFactoryTest {

    private static final String SENDER = "sender";
    private static final String RECEIVER = "receiver";

    @Mock
    private TimestampProvider timestampProvider;

    @InjectMocks
    private EmailFactory sut;

    @ParameterizedTest
    @ValueSource(strings = {"mail-with-subject.eml", "mail-with-subject-without-content-type.eml", "multipart-mail-plain-only.eml"})
    void shouldCreateMailPlainTextEmails(String testFilename) throws Exception {
        var now = new Date();
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var dataAsString = new String(data, StandardCharsets.UTF_8);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertPlainTextEmail(now, dataAsString, result);
    }

    private void assertPlainTextEmail(Date now, String dataAsString, Email result) {
        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertThat(result.getContents(), hasSize(1));
        assertFalse(result.getHtmlContent().isPresent());
        assertTrue(result.getPlainContent().isPresent());
        assertEquals("This is the message content", result.getPlainContent().get().getData());
        assertEquals(now, result.getReceivedOn());
        assertThat(result.getAttachments(), empty());
    }

    @Test
    void shouldCreateEmailForEmlFileWithSubjectAndContentTypeHtml() throws Exception {
        var now = new Date();
        var testFilename = "mail-with-subject-and-content-type-html.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var dataAsString = new String(data, StandardCharsets.UTF_8);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertThat(result.getContents(), hasSize(1));
        assertFalse(result.getPlainContent().isPresent());
        assertTrue(result.getHtmlContent().isPresent());
        assertEquals("<html><head></head><body>Mail Body</body></html>", result.getHtmlContent().get().getData());
        assertEquals(now, result.getReceivedOn());
        assertThat(result.getAttachments(), empty());
    }

    @Test
    void shouldCreateEmailForEmlFileWithSubjectAndContentTypeHtmlAndEmbeddedImage() throws Exception {
        var now = new Date();
        var testFilename = "mail-with-subect-and-content-type-html-with-inline-image.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var dataAsString = new String(data, StandardCharsets.UTF_8);
        var rawData = new RawData(SENDER, RECEIVER, data);
        final var imageBase64 = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAMAAADXqc3KAAAAolBMVEUAAAA0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNuo1pBsAAAANXRSTlMAAQMEBQYHCQsMDQ4PEB0eKi0uOTs8PT5KTGdpa2xzdHuGiKiqq621t7m+xcfO2dze4unt+xTcEm4AAACdSURBVCjPrY/JEsFQFAX7IcQ8xDzPhBiT8/+/ZoFQPAtK72533cWBr8lvZMHPsZQVn7M9nBjbw5Dk3uYPKcgE7z7IQAPTe/UDQx31DaXDsz6WMV0hBUUSk4efJihsJSRFHaiGVx3WoB3pGqR5GmchSXOH9EyKg8IKeLudB5XbK/EiA2BG9zsO2jddt/VYiz7wQ1jb/Yqcb/PrLP/jAkeJUZTlAz/+AAAAAElFTkSuQmCC";

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertThat(result.getContents(), hasSize(2));
        assertTrue(result.getPlainContent().isPresent());
        assertEquals("This is the test mail", result.getPlainContent().get().getData());
        assertTrue(result.getHtmlContent().isPresent());
        assertEquals("<html><head></head><body>This is the test mail <img src=\"cid:icon\"></img></body>", result.getHtmlContent().get().getData());
        assertEquals(now, result.getReceivedOn());
        assertThat(result.getAttachments(), empty());
        assertThat(result.getInlineImages(), hasSize(1));
        assertEquals(imageBase64, result.getInlineImages().get(0).getData());
        assertEquals("image/png", result.getInlineImages().get(0).getContentType());
    }

    @Test
    void shouldCreateEmailForEmlFileWithoutSubjectAndContentTypePlain() throws Exception {
        var now = new Date();
        var testFilename = "mail-without-subject.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var dataAsString = new String(data, StandardCharsets.UTF_8);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals(EmailFactory.UNDEFINED, result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertThat(result.getContents(), hasSize(1));
        assertFalse(result.getHtmlContent().isPresent());
        assertTrue(result.getPlainContent().isPresent());
        assertEquals("This is the message content", result.getPlainContent().get().getData());
        assertEquals(now, result.getReceivedOn());
        assertThat(result.getAttachments(), empty());
    }

    @Test
    void shouldCreateMailForPlainText() throws Exception {
        var now = new Date();
        var dataAsString = "this is just some dummy content";
        var data = dataAsString.getBytes(StandardCharsets.UTF_8);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals(EmailFactory.UNDEFINED, result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertThat(result.getContents(), hasSize(1));
        assertFalse(result.getHtmlContent().isPresent());
        assertTrue(result.getPlainContent().isPresent());
        assertEquals(dataAsString, result.getPlainContent().get().getData());
        assertEquals(now, result.getReceivedOn());
        assertThat(result.getAttachments(), empty());
    }

    @Test
    void shouldCreateMailForMultipartWithContentTypeHtmlAndPlain() throws Exception {
        var now = new Date();
        var testFilename = "multipart-mail.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var dataAsString = new String(data, StandardCharsets.UTF_8);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertThat(result.getContents(), hasSize(2));
        assertTrue(result.getHtmlContent().isPresent());
        assertTrue(result.getPlainContent().isPresent());
        assertEquals("This is the message content", result.getPlainContent().get().getData());
        assertEquals("<html><head></head><body>Mail Body</body></html>", result.getHtmlContent().get().getData());
        assertEquals(now, result.getReceivedOn());
        assertThat(result.getAttachments(), empty());
    }

    @Test
    void shouldCreateMailForMultipartWithUnknownContentType() throws Exception {
        var now = new Date();
        var testFilename = "multipart-mail-unknown-content-type.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var dataAsString = new String(data, StandardCharsets.UTF_8);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertThat(result.getContents(), hasSize(2));
        assertFalse(result.getHtmlContent().isPresent());
        assertTrue(result.getPlainContent().isPresent());
        assertThat(result.getContents().stream().map(EmailContent::getContentType).collect(toList()), contains(ContentType.PLAIN, ContentType.PLAIN));
        assertThat(result.getContents().stream().map(EmailContent::getData).collect(toList()), containsInAnyOrder("This is the message content 1", "This is the message content 2"));
        assertEquals(now, result.getReceivedOn());
        assertThat(result.getAttachments(), empty());
    }

    @Test
    void shouldCreateMailForMultipartWithPlainAndHtmlContentAndAttachments() throws Exception {
        var now = new Date();
        var testFilename = "multipart-mail-html-and-plain-with-attachments.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var dataAsString = new String(data, StandardCharsets.UTF_8);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("Test-Alternative-Mail 4", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertThat(result.getContents(), hasSize(2));
        assertTrue(result.getHtmlContent().isPresent());
        assertTrue(result.getPlainContent().isPresent());
        assertEquals("This is the test mail number4", result.getPlainContent().get().getData());
        assertEquals("<html><head></head><body>This is the test mail number 4</body>", result.getHtmlContent().get().getData());
        assertEquals(now, result.getReceivedOn());
        assertThat(result.getAttachments(), hasSize(2));
        assertThat(result.getAttachments().stream().map(EmailAttachment::getFilename).collect(toList()), containsInAnyOrder("customizing.css", "app-icon.png"));
    }
}