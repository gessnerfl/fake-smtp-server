package de.gessnerfl.fakesmtp.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import de.gessnerfl.fakesmtp.model.Email;
import de.gessnerfl.fakesmtp.model.EmailAttachment;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.service.EmailSseEmitterService;
import de.gessnerfl.fakesmtp.util.MediaTypeUtil;
import jakarta.servlet.ServletContext;

@ExtendWith(MockitoExtension.class)
class EmailRestControllerTest {
	@Mock
	private EmailRepository emailRepository;
	@Mock
	private EmailAttachmentRepository emailAttachmentRepository;
	@Mock
	private MediaTypeUtil mediaTypeUtil;
	@Mock
	private ServletContext servletContext;
	@Mock
	private EmailSseEmitterService emailSseEmitterService;

	@InjectMocks
	private EmailRestController sut;

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
		assertEquals("attachment;filename=myfile.txt", result.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0));
		assertEquals(mediaType.toString(), result.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
		assertEquals(fileContent.length + "", result.getHeaders().get(HttpHeaders.CONTENT_LENGTH).get(0));
		assertArrayEquals(fileContent, result.getBody().getByteArray());
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

		assertThrows(AttachmentNotFoundException.class, () -> {
			sut.getEmailAttachmentById(emailId, attachmentId);
		});
	}

	@Test
	void shouldThrowExceptionWhenAttachmentExistsForTheGivenIdButTheEmailIdDoesNotMatch() {
		var emailId = 123L;
		var attachmentId = 456L;

		when(emailAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

		assertThrows(AttachmentNotFoundException.class, () -> {
			sut.getEmailAttachmentById(emailId, attachmentId);
		});
	}

	@Test
	void shouldDeleteEmailByItsIdAndFlushChangesSoThatDeleteIsApplied() {
		var emailId = 123L;

		sut.deleteEmailById(emailId);

		verify(emailRepository).deleteById(emailId);
		verify(emailRepository).flush();
	}

	@Test
	void shouldDeleteAllEmails() {
		sut.deleteAllEmails();

		verify(emailAttachmentRepository).deleteAllInBatch();
		verify(emailRepository).deleteAllInBatch();
		verify(emailRepository).flush();
		verifyNoMoreInteractions(emailRepository);
	}

	@Test
	void shouldReturnSseEmitterWhenSubscribingToEmailEvents() throws IOException {
		final var emitter = mock(SseEmitter.class);
		when(emailSseEmitterService.createAndAddEmitter()).thenReturn(emitter);

		SseEmitter result = sut.subscribeToEmailEvents();

		assertSame(result, emitter);
		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
		verify(emitter, never()).complete();
		verifyNoMoreInteractions(emailSseEmitterService, emitter);
	}

	@Test
	void shouldSendInitialEventWhenSubscribingToEmailEvents() throws IOException {
		final var emitter = mock(SseEmitter.class);
		when(emailSseEmitterService.createAndAddEmitter()).thenReturn(emitter);

		SseEmitter result = sut.subscribeToEmailEvents();

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
			assertTrue(dataRaw instanceof String);
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

		SseEmitter result = sut.subscribeToEmailEvents();

		assertNotNull(result);
		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
		verify(emitter).complete();
		verifyNoMoreInteractions(emailSseEmitterService, emitter);
	}

}
