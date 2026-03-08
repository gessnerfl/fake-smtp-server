package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.TestResourceUtil;
import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import de.gessnerfl.fakesmtp.model.ContentType;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailContent;
import de.gessnerfl.fakesmtp.model.EmailPartProcessingStatus;
import de.gessnerfl.fakesmtp.util.TimestampProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

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

    private FakeSmtpConfigurationProperties configurationProperties;
    private EmailFactory sut;

    @BeforeEach
    void setUp() {
        configurationProperties = new FakeSmtpConfigurationProperties();
        configurationProperties.setMaxAttachmentSize(DataSize.ofMegabytes(10));
        sut = new EmailFactory(timestampProvider, configurationProperties);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mail-with-subject.eml", "mail-with-subject-without-content-type.eml", "multipart-mail-plain-only.eml"})
    void shouldCreateMailPlainTextEmails(String testFilename) throws Exception {
        var now = getUtcNow();
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var dataAsString = new String(data, StandardCharsets.UTF_8);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertPlainTextEmail(now, dataAsString, result);
    }

    private void assertPlainTextEmail(ZonedDateTime now, String dataAsString, Email result) {
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
        var now = getUtcNow();
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
        var now = getUtcNow();
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
        assertEquals(imageBase64, result.getInlineImages().getFirst().getData());
        assertEquals("icon", result.getInlineImages().getFirst().getContentId());
        assertEquals("image/png", result.getInlineImages().getFirst().getContentType());
    }

    @Test
    void shouldKeepUnwrappedContentIdForInlineImage() throws Exception {
        var now = getUtcNow();
        var rawData = createRawDataWithInlineImageContentIdHeader("icon");

        when(timestampProvider.now()).thenReturn(now);

        var result = assertDoesNotThrow(() -> sut.convert(rawData));

        assertThat(result.getInlineImages(), hasSize(1));
        assertEquals("icon", result.getInlineImages().getFirst().getContentId());
    }

    @ParameterizedTest
    @MethodSource("defensiveContentIdHeaderValues")
    void shouldHandleMalformedContentIdHeaderDefensively(String headerValue, String expectedContentId) throws Exception {
        var now = getUtcNow();
        var rawData = createRawDataWithInlineImageContentIdHeader(headerValue);

        when(timestampProvider.now()).thenReturn(now);

        var result = assertDoesNotThrow(() -> sut.convert(rawData));

        if (expectedContentId == null) {
            assertThat(result.getInlineImages(), empty());
            return;
        }

        assertThat(result.getInlineImages(), hasSize(1));
        assertEquals(expectedContentId, result.getInlineImages().getFirst().getContentId());
    }

    @Test
    void shouldThrowExceptionWhenInlineImageIsBroken() throws Exception {
        var now = getUtcNow();
        var testFilename = "mail-with-subect-and-content-type-html-with-broken-inline-image.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        assertThrows(EmailProcessingException.class, () -> {
            sut.convert(rawData);
            fail();
        });

    }

    @Test
    void shouldCreateEmailForEmlFileWithoutSubjectAndContentTypePlain() throws Exception {
        var now = getUtcNow();
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
        var now = getUtcNow();
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
        var now = getUtcNow();
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
        var now = getUtcNow();
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
        var now = getUtcNow();
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

    private RawData createRawDataWithInlineImageContentIdHeader(String contentIdHeaderValue) throws Exception {
        var mail = TestResourceUtil.getTestFileContent("mail-with-subect-and-content-type-html-with-inline-image.eml")
                .replace("Content-ID: <icon>", "Content-ID: " + contentIdHeaderValue);
        return new RawData(SENDER, RECEIVER, mail.getBytes(StandardCharsets.UTF_8));
    }

    private static Stream<Arguments> defensiveContentIdHeaderValues() {
        return Stream.of(
                Arguments.of("<", "<"),
                Arguments.of(">", ">"),
                Arguments.of("<icon", "<icon"),
                Arguments.of("icon>", "icon>"),
                Arguments.of("<>", null),
                Arguments.of("< >", null),
                Arguments.of("", null),
                Arguments.of("   ", null)
        );
    }

    private static ZonedDateTime getUtcNow() {
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }

    @Test
    void shouldCorrectlyEncodeBase64DataFromInputStream() throws Exception {
        // Given: EML file with BASE64 encoded inline image
        var now = getUtcNow();
        var testFilename = "mail-with-subect-and-content-type-html-with-inline-image.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var rawData = new RawData(SENDER, RECEIVER, data);

        // Expected BASE64 data (as stored in the EML file, joined without line breaks)
        var expectedBase64Data = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAMAAADXqc3KAAAAolBMVEUAAAA0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNs0mNuo1pBsAAAANXRSTlMAAQMEBQYHCQsMDQ4PEB0eKi0uOTs8PT5KTGdpa2xzdHuGiKiqq621t7m+xcfO2dze4unt+xTcEm4AAACdSURBVCjPrY/JEsFQFAX7IcQ8xDzPhBiT8/+/ZoFQPAtK72533cWBr8lvZMHPsZQVn7M9nBjbw5Dk3uYPKcgE7z7IQAPTe/UDQx31DaXDsz6WMV0hBUUSk4efJihsJSRFHaiGVx3WoB3pGqR5GmchSXOH9EyKg8IKeLudB5XbK/EiA2BG9zsO2jddt/VYiz7wQ1jb/Yqcb/PrLP/jAkeJUZTlAz/+AAAAAElFTkSuQmCC";

        when(timestampProvider.now()).thenReturn(now);

        // When: Convert the email
        var result = sut.convert(rawData);

        // Then: Verify BASE64 encoding worked correctly
        assertThat(result.getInlineImages(), hasSize(1));
        var inlineImage = result.getInlineImages().getFirst();
        var actualBase64Data = inlineImage.getData();

        // Verify the BASE64 data matches expected
        assertEquals(expectedBase64Data, actualBase64Data, "BASE64 encoded data should match expected value");

        // Verify the BASE64 data can be decoded back to binary
        var decodedBytes = java.util.Base64.getDecoder().decode(actualBase64Data);
        assertTrue(decodedBytes.length > 0, "Decoded BASE64 data should not be empty");

        // Verify the decoded data starts with PNG magic bytes (for a PNG image)
        assertEquals((byte) 0x89, decodedBytes[0], "First byte should be PNG magic byte");
        assertEquals((byte) 0x50, decodedBytes[1], "Second byte should be 'P'");
        assertEquals((byte) 0x4E, decodedBytes[2], "Third byte should be 'N'");
        assertEquals((byte) 0x47, decodedBytes[3], "Fourth byte should be 'G'");
    }

    @Test
    void shouldRespectMaxAttachmentSizeLimit() throws Exception {
        // Given: Very small attachment size limit (1 byte)
        configurationProperties.setMaxAttachmentSize(DataSize.ofBytes(1));
        var now = getUtcNow();
        var testFilename = "mail-with-subect-and-content-type-html-with-inline-image.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        // When: Convert the email with size limit
        var result = sut.convert(rawData);

        // Then: The inline image should be skipped with explicit status/message
        assertThat(result.getInlineImages(), hasSize(1));
        var inlineImage = result.getInlineImages().getFirst();
        assertEquals("", inlineImage.getData());
        assertEquals(EmailPartProcessingStatus.SKIPPED_TOO_LARGE, inlineImage.getProcessingStatus());
        assertNotNull(inlineImage.getProcessingMessage());
        assertTrue(inlineImage.getProcessingMessage().contains("SKIPPED_TOO_LARGE"));
    }

    @Test
    void shouldSkipAttachmentWhenMaxAttachmentSizeIsExceeded() throws Exception {
        configurationProperties.setMaxAttachmentSize(DataSize.ofBytes(1));
        var now = getUtcNow();
        var testFilename = "multipart-mail-html-and-plain-with-attachments.eml";
        var data = TestResourceUtil.getTestFileContentBytes(testFilename);
        var rawData = new RawData(SENDER, RECEIVER, data);

        when(timestampProvider.now()).thenReturn(now);

        var result = sut.convert(rawData);

        assertThat(result.getAttachments(), hasSize(2));
        assertThat(result.getAttachments().stream().map(EmailAttachment::getProcessingStatus).collect(toList()),
                everyItem(is(EmailPartProcessingStatus.SKIPPED_TOO_LARGE)));
        assertThat(result.getAttachments().stream().map(EmailAttachment::getData).map(bytes -> bytes.length).collect(toList()),
                everyItem(is(0)));
        assertThat(result.getAttachments().stream().map(EmailAttachment::getProcessingMessage).collect(toList()),
                everyItem(containsString("SKIPPED_TOO_LARGE")));
    }

    @Test
    void shouldNotApplyAttachmentSizeLimitToTopLevelStreamMessageContent() {
        configurationProperties.setMaxAttachmentSize(DataSize.ofBytes(1));
        var now = getUtcNow();
        var rawEmail = String.join("\r\n",
                "From: sender@example.com",
                "To: receiver@example.com",
                "Subject: Stream body",
                "MIME-Version: 1.0",
                "Content-Type: application/octet-stream",
                "Content-Transfer-Encoding: base64",
                "",
                "QUJDRA==",
                "");
        var rawData = new RawData(SENDER, RECEIVER, rawEmail.getBytes(StandardCharsets.UTF_8));

        when(timestampProvider.now()).thenReturn(now);

        var result = assertDoesNotThrow(() -> sut.convert(rawData));

        assertThat(result.getContents(), hasSize(1));
        assertEquals(ContentType.OCTET_STREAM, result.getContents().getFirst().getContentType());
        assertEquals("QUJDRA==", result.getContents().getFirst().getData());
        assertThat(result.getAttachments(), empty());
    }

}
