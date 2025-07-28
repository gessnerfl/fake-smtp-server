package de.gessnerfl.fakesmtp.service;

import de.gessnerfl.fakesmtp.model.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSseEmitterServiceTest {

	@Mock
	private Logger logger;

	@Mock
	private SseEmitter emitter;

	@Mock
	private Email email;

	@InjectMocks
	private EmailSseEmitterService sut;

	@Test
	@SuppressWarnings("unchecked")
	void shouldAddEmitterAndReturnIt() {
		SseEmitter result = sut.add(emitter);

		assertEquals(emitter, result);

		verify(emitter).onCompletion(any(Runnable.class));
		verify(emitter).onTimeout(any(Runnable.class));
		verify(emitter).onError(any(java.util.function.Consumer.class));
		verify(logger).debug(contains("SSE emitter added"), anyInt());
	}

	@Test
	void shouldSendEmailReceivedEventToAllEmitters() throws IOException {
		when(email.getId()).thenReturn(123L);
		sut.add(emitter);

		sut.sendEmailReceivedEvent(email);

		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
		verify(logger, never()).warn(anyString(), any(Exception.class));
	}

	@Test
	void shouldRemoveDeadEmittersWhenSendingEvents() throws IOException {
		when(email.getId()).thenReturn(123L);
		sut.add(emitter);
		doThrow(new IOException("Test exception")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

		sut.sendEmailReceivedEvent(email);

		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
		verify(logger).warn(eq("Failed to send event to emitter"), any(IOException.class));
		verify(logger).debug(contains("Removed {} dead emitters"), eq(1), anyInt());

		reset(emitter, logger);
		sut.sendEmailReceivedEvent(email);
		verifyNoInteractions(emitter);
	}

	@Test
	void shouldHandleCompletionCallback() {
		doAnswer(invocation -> {
			Runnable callback = invocation.getArgument(0);
			callback.run();
			return null;
		}).when(emitter).onCompletion(any(Runnable.class));

		sut.add(emitter);

		verify(logger).debug(contains("SSE emitter completed and removed"), anyInt());
	}

	@Test
	void shouldHandleTimeoutCallback() {
		doAnswer(invocation -> {
			Runnable callback = invocation.getArgument(0);
			callback.run();
			return null;
		}).when(emitter).onTimeout(any(Runnable.class));

		sut.add(emitter);

		verify(emitter).complete();
		verify(logger).debug(contains("SSE emitter timed out and removed"), anyInt());
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldHandleErrorCallback() {
		Exception testException = new RuntimeException("Test exception");
		doAnswer(invocation -> {
			java.util.function.Consumer<Throwable> callback = invocation.getArgument(0);
			callback.accept(testException);
			return null;
		}).when(emitter).onError(any(java.util.function.Consumer.class));

		sut.add(emitter);

		verify(emitter).complete();
		verify(logger).warn(contains("SSE emitter failed and removed"), anyInt(), eq(testException));
	}
}
