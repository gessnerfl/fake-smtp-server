package de.gessnerfl.fakesmtp.server;

import de.gessnerfl.fakesmtp.TestResourceUtil;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
    public void shouldCreateEmailForEmlFileWithSubject() throws Exception {
        Date now = new Date();
        String testFilename = "mail-with-subject.eml";
        InputStream data = TestResourceUtil.getTestFile(testFilename);
        String content = TestResourceUtil.getTestFileContent(testFilename);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(SENDER, RECEIVER, data);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals("This is the mail title", result.getSubject());
        assertEquals(content, result.getContent());
        assertEquals(now, result.getReceivedAt());
    }

    @Test
    public void shouldCreateEmailForEmlFileWithoutSubject() throws Exception {
        Date now = new Date();
        String testFilename = "mail-without-subject.eml";
        InputStream data = TestResourceUtil.getTestFile(testFilename);
        String content = TestResourceUtil.getTestFileContent(testFilename);

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(SENDER, RECEIVER, data);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals(EmailFactory.NO_SUBJECT, result.getSubject());
        assertEquals(content, result.getContent());
        assertEquals(now, result.getReceivedAt());
    }

    @Test
    public void shouldCreateMailForPlainText() throws Exception {
        Date now = new Date();
        String content = "this is just some dummy content";
        InputStream data = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        when(timestampProvider.now()).thenReturn(now);

        Email result = sut.convert(SENDER, RECEIVER, data);

        assertEquals(SENDER, result.getFromAddress());
        assertEquals(RECEIVER, result.getToAddress());
        assertEquals(EmailFactory.NO_SUBJECT, result.getSubject());
        assertEquals(content, result.getContent());
        assertEquals(now, result.getReceivedAt());
    }
}