package de.gessnerfl.fakesmtp.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import de.gessnerfl.fakesmtp.config.WebappAuthenticationProperties;
import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.model.EmailPartProcessingStatus;
import de.gessnerfl.fakesmtp.model.InlineImage;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailContentRepository;
import de.gessnerfl.fakesmtp.repository.EmailInlineImageRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.service.EmailDeletionService;
import de.gessnerfl.fakesmtp.service.EmailSseEmitterService;
import de.gessnerfl.fakesmtp.util.MediaTypeUtil;
import jakarta.servlet.ServletContext;

@ExtendWith(MockitoExtension.class)
class EmailRestControllerTest {
	private static final String INVALID_INLINE_IMAGE_CONTENT_TYPE_MESSAGE = "INVALID_INLINE_IMAGE_CONTENT_TYPE: Stored inline image content type is invalid";
	private static final String INVALID_INLINE_IMAGE_BASE64_MESSAGE = "INVALID_INLINE_IMAGE_BASE64: Stored inline image data is invalid";

	@Mock
	private EmailRepository emailRepository;
	@Mock
	private EmailAttachmentRepository emailAttachmentRepository;
	@Mock
	private EmailContentRepository emailContentRepository;
	@Mock
	private EmailInlineImageRepository emailInlineImageRepository;
	@Mock
	private MediaTypeUtil mediaTypeUtil;
	@Mock
	private ServletContext servletContext;
	@Mock
	private EmailSseEmitterService emailSseEmitterService;
	@Mock
	private EmailDeletionService emailDeletionService;
	@Mock
	private WebappAuthenticationProperties authProperties;

	@InjectMocks
	private EmailRestController sut;

	@BeforeEach
	void setUp() {
		var auth = new UsernamePasswordAuthenticationToken("testuser", "password");
		SecurityContextHolder.getContext().setAuthentication(auth);
		lenient().when(authProperties.isAuthenticationEnabled()).thenReturn(true);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldReturnListOfEmails() {
		@SuppressWarnings("unchecked")
		final Page<Email> page = mock(Page.class);
		when(emailRepository.findAll(any(Pageable.class))).thenReturn(page);

		var pageable = PageRequest.of(0, 5, Sort.Direction.DESC, "receivedOn");
		var result = sut.all(pageable);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(emailRepository).findAll(pageableCaptor.capture());
		assertEquals(pageableCaptor.getValue(), pageable);
		assertEquals(page, result);
		verifyNoMoreInteractions(emailRepository);
	}

	@Test
	void shouldReturnSingleEmailWhenIdIsValid() {
		var id = 12L;
		var mail = mock(Email.class);
		when(emailRepository.findById(id)).thenReturn(Optional.of(mail));

		var result = sut.getEmailById(id);

		assertEquals(mail, result);
		verify(emailRepository).findById(id);
	}

	@Test
	void shouldReturnResponseEntityForAttachment() {
		var fileContent = "this is the file content".getBytes(StandardCharsets.UTF_8);
		var filename = "myfile.txt";
		var emailId = 123L;
		var attachmentId = 456L;
		var email = mock(Email.class);
		var attachment = mock(EmailAttachment.class);
		var mediaType = MediaType.TEXT_PLAIN;

		when(email.getId()).thenReturn(emailId);
		when(attachment.getEmail()).thenReturn(email);
		when(attachment.getFilename()).thenReturn(filename);
		when(attachment.getData()).thenReturn(fileContent);
		when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
		when(mediaTypeUtil.getMediaTypeForFileName(servletContext, filename)).thenReturn(mediaType);

		var result = sut.getEmailAttachmentById(emailId, attachmentId);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		var contentDisposition = result.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION);
		assertNotNull(contentDisposition);
		assertEquals("attachment;filename=myfile.txt", contentDisposition.getFirst());
		var contentType = result.getHeaders().get(HttpHeaders.CONTENT_TYPE);
		assertNotNull(contentType);
		assertEquals(mediaType.toString(), contentType.getFirst());
		var contentLength = result.getHeaders().get(HttpHeaders.CONTENT_LENGTH);
		assertNotNull(contentLength);
		assertEquals(fileContent.length + "", contentLength.getFirst());
		var body = result.getBody();
		assertNotNull(body);
		assertArrayEquals(fileContent, body.getByteArray());
	}

	@Test
	void shouldThrowExceptionWhenNoAttachmentExistsForTheGivenId() {
		var emailId = 123L;
		var attachmentId = 456L;
		var email = mock(Email.class);
		var attachment = mock(EmailAttachment.class);

		when(email.getId()).thenReturn(789L);
		when(attachment.getEmail()).thenReturn(email);
		when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

		assertThrows(AttachmentNotFoundException.class, () -> sut.getEmailAttachmentById(emailId, attachmentId));
	}

	@Test
	void shouldReturn413WhenAttachmentWasSkippedDuringProcessing() {
		var emailId = 123L;
		var attachmentId = 456L;
		var email = mock(Email.class);
		var attachment = mock(EmailAttachment.class);
		var message = "SKIPPED_TOO_LARGE: Attachment 'invoice.pdf' exceeded configured max attachment size of 1B";

		when(email.getId()).thenReturn(emailId);
		when(attachment.getEmail()).thenReturn(email);
		when(attachment.getProcessingStatus()).thenReturn(EmailPartProcessingStatus.SKIPPED_TOO_LARGE);
		when(attachment.getProcessingMessage()).thenReturn(message);
		when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

		var result = sut.getEmailAttachmentById(emailId, attachmentId);

		assertEquals(413, result.getStatusCode().value());
		var body = result.getBody();
		assertNotNull(body);
		assertEquals(message, new String(body.getByteArray(), StandardCharsets.UTF_8));
	}

	@Test
	void shouldThrowExceptionWhenAttachmentExistsForTheGivenIdButTheEmailIdDoesNotMatch() {
		var emailId = 123L;
		var attachmentId = 456L;

		when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

		assertThrows(AttachmentNotFoundException.class, () -> sut.getEmailAttachmentById(emailId, attachmentId));
	}

	@Test
	void shouldReturnResponseEntityForInlineImage() {
		var inlineImageBytes = "fake-image-data".getBytes(StandardCharsets.UTF_8);
		var inlineImageData = Base64.getEncoder().encodeToString(inlineImageBytes);
		var emailId = 123L;
		var inlineImageId = 456L;
		var email = mock(Email.class);
		var inlineImage = mock(InlineImage.class);

		when(email.getId()).thenReturn(emailId);
		when(inlineImage.getEmail()).thenReturn(email);
		when(inlineImage.getContentType()).thenReturn(MediaType.IMAGE_PNG_VALUE);
		when(inlineImage.getData()).thenReturn(inlineImageData);
		when(emailInlineImageRepository.findById(inlineImageId)).thenReturn(Optional.of(inlineImage));

		var result = sut.getEmailInlineImageById(emailId, inlineImageId);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		var contentType = result.getHeaders().get(HttpHeaders.CONTENT_TYPE);
		assertNotNull(contentType);
		assertEquals(MediaType.IMAGE_PNG_VALUE, contentType.getFirst());
		var body = result.getBody();
		assertNotNull(body);
		assertArrayEquals(inlineImageBytes, body.getByteArray());
	}

	@Test
	void shouldReturn413WhenInlineImageWasSkippedDuringProcessing() {
		var emailId = 123L;
		var inlineImageId = 456L;
		var email = mock(Email.class);
		var inlineImage = mock(InlineImage.class);
		var message = "SKIPPED_TOO_LARGE: Inline image exceeded configured max attachment size";

		when(email.getId()).thenReturn(emailId);
		when(inlineImage.getEmail()).thenReturn(email);
		when(inlineImage.getProcessingStatus()).thenReturn(EmailPartProcessingStatus.SKIPPED_TOO_LARGE);
		when(inlineImage.getProcessingMessage()).thenReturn(message);
		when(emailInlineImageRepository.findById(inlineImageId)).thenReturn(Optional.of(inlineImage));

		var result = sut.getEmailInlineImageById(emailId, inlineImageId);

		assertEquals(413, result.getStatusCode().value());
		var body = result.getBody();
		assertNotNull(body);
		assertEquals(message, new String(body.getByteArray(), StandardCharsets.UTF_8));
	}

	@ParameterizedTest
	@MethodSource("invalidInlineImageBase64Values")
	void shouldReturn422WhenInlineImageDataIsInvalidBase64(String inlineImageData) {
		var emailId = 123L;
		var inlineImageId = 456L;
		setUpInlineImage(emailId, inlineImageId, MediaType.IMAGE_PNG_VALUE, inlineImageData);

		var result = sut.getEmailInlineImageById(emailId, inlineImageId);

		assertUnprocessableInlineImageResponse(result, INVALID_INLINE_IMAGE_BASE64_MESSAGE);
	}

	@Test
	void shouldReturn422WhenInlineImageDataIsNull() {
		var emailId = 123L;
		var inlineImageId = 456L;
		setUpInlineImage(emailId, inlineImageId, MediaType.IMAGE_PNG_VALUE, null);

		var result = sut.getEmailInlineImageById(emailId, inlineImageId);

		assertUnprocessableInlineImageResponse(result, INVALID_INLINE_IMAGE_BASE64_MESSAGE);
	}

	@ParameterizedTest
	@MethodSource("invalidInlineImageContentTypes")
	void shouldReturn422WhenInlineImageContentTypeIsInvalid(String inlineImageContentType) {
		var emailId = 123L;
		var inlineImageId = 456L;
		var email = mock(Email.class);
		var inlineImage = mock(InlineImage.class);

		when(email.getId()).thenReturn(emailId);
		when(inlineImage.getEmail()).thenReturn(email);
		when(inlineImage.getContentType()).thenReturn(inlineImageContentType);
		when(emailInlineImageRepository.findById(inlineImageId)).thenReturn(Optional.of(inlineImage));

		var result = sut.getEmailInlineImageById(emailId, inlineImageId);

		assertUnprocessableInlineImageResponse(result, INVALID_INLINE_IMAGE_CONTENT_TYPE_MESSAGE);
	}

	@Test
	void shouldDeleteEmailByItsIdAndFlushChangesSoThatDeleteIsApplied() {
		var emailId = 123L;

		sut.deleteEmailById(emailId);

		verify(emailDeletionService).deleteEmailById(emailId);
	}

	@Test
	void shouldThrowExceptionWhenDeletingNonExistentEmail() {
		var emailId = 123L;
		doThrow(new EmailNotFoundException("Could not find email " + emailId))
				.when(emailDeletionService)
				.deleteEmailById(emailId);

		assertThrows(EmailNotFoundException.class, () -> sut.deleteEmailById(emailId));
	}

	@Test
	void shouldDeleteAllEmails() {
		sut.deleteAllEmails();

		verify(emailDeletionService).deleteAllEmails();
	}

	@Test
	void shouldReturnSseEmitterWhenSubscribingToEmailEvents() throws IOException {
		final var emitter = mock(SseEmitter.class);
		when(emailSseEmitterService.createAndAddEmitter()).thenReturn(emitter);
		
		var principal = mock(Principal.class);
		SseEmitter result = sut.subscribeToEmailEvents(principal);

		assertSame(result, emitter);
		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
		verify(emitter, never()).complete();
		verifyNoMoreInteractions(emailSseEmitterService, emitter);
	}

	@Test
	void shouldSendInitialEventWhenSubscribingToEmailEvents() throws IOException {
		final var emitter = mock(SseEmitter.class);
		when(emailSseEmitterService.createAndAddEmitter()).thenReturn(emitter);

		var principal = mock(Principal.class);
		SseEmitter result = sut.subscribeToEmailEvents(principal);

		assertNotNull(result);
		ArgumentCaptor<SseEmitter.SseEventBuilder> eventCaptor = ArgumentCaptor
				.forClass(SseEmitter.SseEventBuilder.class);
		verify(result).send(eventCaptor.capture());
		verify(emitter, never()).complete();
		verifyNoMoreInteractions(emailSseEmitterService, emitter);

		var eventBuilder = eventCaptor.getValue();
		assertNotNull(eventBuilder);
		var event = eventBuilder.build();
		assertEquals(3, event.size());
		var containsEventElement = false;
		var containsDataElement = false;
		var containsEmptyElement = false;
		for (var element : event) {
			var dataRaw = element.getData();
			assertInstanceOf(String.class, dataRaw);
			var data = (String) dataRaw;
			if (data.startsWith("event:")) {
				assertEquals("event:connection-established\ndata:", data);
				containsEventElement = true;
			} else {
				if (data.isBlank()) {
					assertEquals("\n\n", data);
					containsEmptyElement = true;
				} else {
					assertEquals("Connected to email events", data);
					containsDataElement = true;
				}
			}
		}
		assertTrue(containsEventElement);
		assertTrue(containsDataElement);
		assertTrue(containsEmptyElement);
	}

	@Test
	void shouldCompleteEmitterWhenSendingInitialEventFails() throws IOException {
		final var emitter = mock(SseEmitter.class);
		doThrow(new IOException("Test exception")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
		when(emailSseEmitterService.createAndAddEmitter()).thenReturn(emitter);

		var principal = mock(Principal.class);
		SseEmitter result = sut.subscribeToEmailEvents(principal);

		assertNotNull(result);
		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
		verify(emitter).complete();
		verifyNoMoreInteractions(emailSseEmitterService, emitter);
	}

	private void setUpInlineImage(long emailId, long inlineImageId, String contentType, String data) {
		var email = mock(Email.class);
		var inlineImage = mock(InlineImage.class);

		when(email.getId()).thenReturn(emailId);
		when(inlineImage.getEmail()).thenReturn(email);
		when(inlineImage.getContentType()).thenReturn(contentType);
		when(inlineImage.getData()).thenReturn(data);
		when(emailInlineImageRepository.findById(inlineImageId)).thenReturn(Optional.of(inlineImage));
	}

	private void assertUnprocessableInlineImageResponse(ResponseEntity<ByteArrayResource> result, String expectedMessage) {
		assertEquals(422, result.getStatusCode().value());
		var contentType = result.getHeaders().get(HttpHeaders.CONTENT_TYPE);
		assertNotNull(contentType);
		assertEquals(MediaType.TEXT_PLAIN_VALUE, contentType.getFirst());
		var body = result.getBody();
		assertNotNull(body);
		assertEquals(expectedMessage, new String(body.getByteArray(), StandardCharsets.UTF_8));
	}

	private static Stream<Arguments> invalidInlineImageBase64Values() {
		return Stream.of(
				Arguments.of("%%%invalid%%%base64%%%"),
				Arguments.of("not-base64!"),
				Arguments.of("%%%%")
		);
	}

	private static Stream<Arguments> invalidInlineImageContentTypes() {
		return Stream.of(
				Arguments.of("invalid-content-type"),
				Arguments.of("text/"),
				Arguments.of("/png"),
				Arguments.of((String) null)
		);
	}

}
