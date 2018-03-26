package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.TestResourceUtil;
import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailFactoryTest {

    private static final String SENDER = "sender";
    private static final String RECEIVER = "receiver";

    @Mock
    private TimestampProvider timestampProvider;

    @InjectMocks
    private EmailFactory sut;

    @Test
    public void shouldCreateEmailForEmlFileWithSubjectAndContentTypePlain() throws Exception {
        Date now = new Date();
        String testFilename = "mail-with-subject.eml";
        byte[] data = TestResourceUtil.getTestFileContentBytes(testFilename);
        String dataAsString = new String(data, StandardCharsets.UTF_8);
        RawData rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertEquals("This is the message content", result.getContent());
        assertEquals(now, result.getReceivedOn());
        assertEquals(ContentType.PLAIN, result.getContentType());
    }

    @Test
    public void shouldCreateEmailForEmlFileWithSubjectAndContentTypeHtml() throws Exception {
        Date now = new Date();
        String testFilename = "mail-with-subject-and-content-type-html.eml";
        byte[] data = TestResourceUtil.getTestFileContentBytes(testFilename);
        String dataAsString = new String(data, StandardCharsets.UTF_8);
        RawData rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertEquals("<html><head></head><body>Mail Body</body></html>", result.getContent());
        assertEquals(now, result.getReceivedOn());
        assertEquals(ContentType.HTML, result.getContentType());
    }

    @Test
    public void shouldCreateEmailForEmlFileWithSubjectAndWithoutContentType() throws Exception {
        Date now = new Date();
        String testFilename = "mail-with-subject-without-content-type.eml";
        byte[] data = TestResourceUtil.getTestFileContentBytes(testFilename);
        String dataAsString = new String(data, StandardCharsets.UTF_8);
        RawData rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertEquals("This is the message content", result.getContent());
        assertEquals(now, result.getReceivedOn());
        assertEquals(ContentType.PLAIN, result.getContentType());
    }

    @Test
    public void shouldCreateEmailForEmlFileWithoutSubjectAndContentTypePlain() throws Exception {
        Date now = new Date();
        String testFilename = "mail-without-subject.eml";
        byte[] data = TestResourceUtil.getTestFileContentBytes(testFilename);
        String dataAsString = new String(data, StandardCharsets.UTF_8);
        RawData rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals(EmailFactory.UNDEFINED, result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertEquals("This is the message content", result.getContent());
        assertEquals(now, result.getReceivedOn());
        assertEquals(ContentType.PLAIN, result.getContentType());
    }

    @Test
    public void shouldCreateMailForPlainText() throws Exception {
        Date now = new Date();
        String dataAsString = "this is just some dummy content";
        byte[] data = dataAsString.getBytes(StandardCharsets.UTF_8);
        RawData rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals(EmailFactory.UNDEFINED, result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertEquals(dataAsString, result.getContent());
        assertEquals(now, result.getReceivedOn());
        assertEquals(ContentType.PLAIN, result.getContentType());
    }

    @Test
    public void shouldCreateMailForMultipartWithContentTypeHtml() throws Exception {
        Date now = new Date();
        String testFilename = "multipart-mail.eml";
        byte[] data = TestResourceUtil.getTestFileContentBytes(testFilename);
        String dataAsString = new String(data, StandardCharsets.UTF_8);
        RawData rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertEquals("<html><head></head><body>Mail Body</body></html>", result.getContent());
        assertEquals(now, result.getReceivedOn());
        assertEquals(ContentType.HTML, result.getContentType());
    }

    @Test
    public void shouldCreateMailForMultipartWithoutContentTypeHtml() throws Exception {
        Date now = new Date();
        String testFilename = "multipart-mail-plain-only.eml";
        byte[] data = TestResourceUtil.getTestFileContentBytes(testFilename);
        String dataAsString = new String(data, StandardCharsets.UTF_8);
        RawData rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertEquals("This is the message content", result.getContent());
        assertEquals(now, result.getReceivedOn());
        assertEquals(ContentType.PLAIN, result.getContentType());
    }

    @Test
    public void shouldCreateMailForMultipartWithUnknownContentType() throws Exception {
        Date now = new Date();
        String testFilename = "multipart-mail-unknown-content-type.eml";
        byte[] data = TestResourceUtil.getTestFileContentBytes(testFilename);
        String dataAsString = new String(data, StandardCharsets.UTF_8);
        RawData rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(rawData);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(dataAsString, result.getRawData());
        assertEquals("This is the message content", result.getContent());
        assertEquals(now, result.getReceivedOn());
        assertEquals(ContentType.PLAIN, result.getContentType());
    }
}