package de.gessnerfl.fakesmtp.service;

import de.gessnerfl.fakesmtp.config.WebappSessionProperties;
import de.gessnerfl.fakesmtp.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSseEmitterServiceTest {

	private static final int HEARTBEAT_INTERVAL_SECONDS = 30;
	private static final int EVENT_SEND_TIMEOUT_SECONDS = 5;

	@Mock
	private TaskScheduler taskScheduler;

	@Mock
	private Logger logger;

	@Mock
	private WebappSessionProperties sessionProperties;

	@Mock
	private SseEmitter emitter;

	@SuppressWarnings("rawtypes")
	@Mock
	private ScheduledFuture scheduledFuture;

	@Mock
	private Email email;

	private EmailSseEmitterService sut;

	@BeforeEach
	void setUp() {
		when(sessionProperties.getSseHeartbeatIntervalSeconds()).thenReturn(HEARTBEAT_INTERVAL_SECONDS);
		when(sessionProperties.getSseEventSendTimeoutSeconds()).thenReturn(EVENT_SEND_TIMEOUT_SECONDS);
		sut = new EmailSseEmitterService(taskScheduler, logger, sessionProperties);
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldAddEmitterAndReturnIt() {
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);

		SseEmitter result = sut.add(emitter);

		assertEquals(emitter, result);

		verify(emitter).onCompletion(any(Runnable.class));
		verify(emitter).onTimeout(any(Runnable.class));
		verify(emitter).onError(any(java.util.function.Consumer.class));
		verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(Instant.class), eq(Duration.ofSeconds(HEARTBEAT_INTERVAL_SECONDS)));
		verify(logger).debug(contains("SSE emitter added"), anyInt());
		verify(logger).debug(contains("Started heartbeat"), anyInt(), anyInt());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldStartHeartbeatWithConfiguredInterval() {
		int customInterval = 60;
		when(sessionProperties.getSseHeartbeatIntervalSeconds()).thenReturn(customInterval);
		EmailSseEmitterService customSut = new EmailSseEmitterService(taskScheduler, logger, sessionProperties);

		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);

		customSut.add(emitter);

		verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(Instant.class), eq(Duration.ofSeconds(customInterval)));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldUseConfiguredEventSendTimeout() {
		int customTimeout = 10;
		when(sessionProperties.getSseEventSendTimeoutSeconds()).thenReturn(customTimeout);
		EmailSseEmitterService customSut = new EmailSseEmitterService(taskScheduler, logger, sessionProperties);
		
		// Verify the service was created with custom timeout
		assertNotNull(customSut);
		verify(sessionProperties, atLeastOnce()).getSseEventSendTimeoutSeconds();
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldSendHeartbeatPeriodically() throws IOException {
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);

		sut.add(emitter);

		ArgumentCaptor<Runnable> heartbeatCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).scheduleAtFixedRate(heartbeatCaptor.capture(), any(Instant.class), any(Duration.class));

		Runnable heartbeatTask = heartbeatCaptor.getValue();
		heartbeatTask.run();

		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
		verify(logger).debug("Sent heartbeat ping to emitter");
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldStopHeartbeatOnIOException() throws IOException {
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);
		doThrow(new IOException("Connection closed")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

		sut.add(emitter);

		ArgumentCaptor<Runnable> heartbeatCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).scheduleAtFixedRate(heartbeatCaptor.capture(), any(Instant.class), any(Duration.class));

		Runnable heartbeatTask = heartbeatCaptor.getValue();
		heartbeatTask.run();

		verify(scheduledFuture).cancel(false);
		verify(logger).debug("Failed to send heartbeat - client likely disconnected");
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldStopHeartbeatOnCompletion() {
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);

		doAnswer(invocation -> {
			Runnable callback = invocation.getArgument(0);
			callback.run();
			return null;
		}).when(emitter).onCompletion(any(Runnable.class));

		sut.add(emitter);

		verify(scheduledFuture).cancel(false);
		verify(logger).debug(contains("Stopped heartbeat"), anyInt());
		verify(logger).debug(contains("SSE emitter completed and removed"), anyInt());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldStopHeartbeatOnTimeout() {
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);

		doAnswer(invocation -> {
			Runnable callback = invocation.getArgument(0);
			callback.run();
			return null;
		}).when(emitter).onTimeout(any(Runnable.class));

		sut.add(emitter);

		verify(emitter).complete();
		verify(scheduledFuture).cancel(false);
		verify(logger).debug(contains("Stopped heartbeat"), anyInt());
		verify(logger).debug(contains("SSE emitter timed out and removed"), anyInt());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldStopHeartbeatOnError() {
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);

		Exception testException = new RuntimeException("Test exception");
		doAnswer(invocation -> {
			java.util.function.Consumer<Throwable> callback = invocation.getArgument(0);
			callback.accept(testException);
			return null;
		}).when(emitter).onError(any(java.util.function.Consumer.class));

		sut.add(emitter);

		verify(emitter).complete();
		verify(scheduledFuture).cancel(false);
		verify(logger).debug(contains("Stopped heartbeat"), anyInt());
		verify(logger).warn(contains("SSE emitter failed and removed"), anyInt(), eq(testException));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldSendEmailReceivedEventToAllEmitters() throws IOException {
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);
		when(email.getId()).thenReturn(123L);

		sut.add(emitter);

		sut.sendEmailReceivedEvent(email);

		// Virtual threads are used, so we need to wait a bit for async execution
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldRemoveDeadEmittersWhenSendingEvents() throws IOException {
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);
		when(email.getId()).thenReturn(123L);

		sut.add(emitter);
		doThrow(new IOException("Test exception")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

		sut.sendEmailReceivedEvent(email);

		// Virtual threads are used, so we need to wait for async execution
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// Emitter should be marked as dead and removed
		verify(scheduledFuture, atLeastOnce()).cancel(false);
		
		reset(emitter, scheduledFuture);
		sut.sendEmailReceivedEvent(email);
		verifyNoInteractions(emitter);
	}

	@Test
	void shouldCreateEmitterWithDefaultTimeout() {
		SseEmitter result = sut.createEmitter();

		assertNotNull(result);
		assertEquals(3600000L, result.getTimeout());
	}

	@Test
	void shouldCreateEmitterWithSpecifiedTimeout() {
		long customTimeout = 1800000L;

		SseEmitter result = sut.createEmitter(customTimeout);

		assertNotNull(result);
		assertEquals(customTimeout, result.getTimeout());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldCreateAndAddEmitterWithDefaultTimeout() {
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);

		SseEmitter result = sut.createAndAddEmitter();

		assertNotNull(result);
		assertEquals(3600000L, result.getTimeout());
		verify(logger).debug(contains("SSE emitter added"), anyInt());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void shouldCreateAndAddEmitterWithSpecifiedTimeout() {
		long customTimeout = 1800000L;
		when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class)))
			.thenReturn(scheduledFuture);

		SseEmitter result = sut.createAndAddEmitter(customTimeout);

		assertNotNull(result);
		assertEquals(customTimeout, result.getTimeout());
		verify(logger).debug(contains("SSE emitter added"), anyInt());
	}

	@Test
	void shouldHandleEmptyEmittersList() {
		// Should not throw when no emitters are connected
		sut.sendEmailReceivedEvent(email);
		
		// No exception should be thrown, no interactions with emitter
		verifyNoInteractions(emitter);
	}
}
