package de.gessnerfl.fakesmtp.smtp.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import de.gessnerfl.fakesmtp.config.SmtpCommandConfig;
import de.gessnerfl.fakesmtp.smtp.RejectException;
import de.gessnerfl.fakesmtp.smtp.client.SMTPException;
import de.gessnerfl.fakesmtp.smtp.client.SmartClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import de.gessnerfl.fakesmtp.smtp.MessageContext;
import de.gessnerfl.fakesmtp.smtp.MessageHandler;
import de.gessnerfl.fakesmtp.smtp.MessageHandlerFactory;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 * This class tests whether the event handler methods defined in MessageHandler
 * are called at the appropriate times and in good order.
 */
class MessageHandlerTest {
	@Mock
	private MessageHandlerFactory messageHandlerFactory;
	@Mock
	private MessageHandler messageHandler;
	@Mock
	private MessageHandler messageHandler2;

	private BaseSmtpServer smtpServer;

	@BeforeEach
	public void setup() throws NoSuchAlgorithmException {
		MockitoAnnotations.openMocks(this);
		int randomPort = SecureRandom.getInstanceStrong().nextInt(1024,65536);
		smtpServer = new BaseSmtpServer("FakeSMTPServer", messageHandlerFactory, new SmtpCommandConfig().commandHandler());
		smtpServer.setPort(randomPort);
		smtpServer.start();
	}

	@AfterEach
	public void teardown() {
		smtpServer.stop();
	}

	@Test
	void testCompletedMailTransaction() throws Exception {
		when(messageHandlerFactory.create(any(MessageContext.class))).thenReturn(messageHandler);

		final SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");
		client.from("john@example.com");
		client.to("jane@example.com");
		client.dataStart();
		client.dataWrite("body".getBytes(StandardCharsets.US_ASCII), 4);
		client.dataEnd();
		client.quit();
		smtpServer.stop(); // wait for the server to catch up

		verify(messageHandlerFactory).create(any(MessageContext.class));
		verify(messageHandler).from(anyString());
		verify(messageHandler).recipient(anyString());
		verify(messageHandler).data(any(InputStream.class));
		verify(messageHandler).done();
		verifyNoMoreInteractions(messageHandlerFactory, messageHandler, messageHandler2);
	}

	@Test
	void testDisconnectImmediately() throws Exception {
		final SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");
		client.quit();
		smtpServer.stop(); // wait for the server to catch up

		verifyNoInteractions(messageHandlerFactory, messageHandler, messageHandler2);
	}

	@Test
	void testAbortedMailTransaction() throws Exception {
		when(messageHandlerFactory.create(any(MessageContext.class))).thenReturn(messageHandler);

		final SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");
		client.from("john@example.com");
		client.quit();
		smtpServer.stop(); // wait for the server to catch up

		verify(messageHandlerFactory).create(any(MessageContext.class));
		verify(messageHandler).from(anyString());
		verify(messageHandler).done();
		verifyNoMoreInteractions(messageHandlerFactory, messageHandler, messageHandler2);
	}

	@Test
	void testTwoMailsInOneSession() throws Exception {
		when(messageHandlerFactory.create(any(MessageContext.class))).thenReturn(messageHandler, messageHandler2);

		final SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");

		client.from("john1@example.com");
		client.to("jane1@example.com");
		client.dataStart();
		client.dataWrite("body1".getBytes(StandardCharsets.US_ASCII), 5);
		client.dataEnd();

		client.from("john2@example.com");
		client.to("jane2@example.com");
		client.dataStart();
		client.dataWrite("body2".getBytes(StandardCharsets.US_ASCII), 5);
		client.dataEnd();

		client.quit();

		smtpServer.stop(); // wait for the server to catch up

		verify(messageHandlerFactory, times(2)).create(any(MessageContext.class));
		verify(messageHandler).from(anyString());
		verify(messageHandler).recipient(anyString());
		verify(messageHandler).data(any(InputStream.class));
		verify(messageHandler).done();
		verify(messageHandler2).from(anyString());
		verify(messageHandler2).recipient(anyString());
		verify(messageHandler2).data(any(InputStream.class));
		verify(messageHandler2).done();
		verifyNoMoreInteractions(messageHandlerFactory, messageHandler, messageHandler2);
	}

	/**
	 * Test for issue 56: rejecting a Mail From causes IllegalStateException in the
	 * next Mail From attempt.
	 *
	 * @throws IOException        on IO error
	 */
	@Test
	void testMailFromRejectedFirst() throws IOException {
		when(messageHandlerFactory.create(any(MessageContext.class))).thenReturn(messageHandler, messageHandler2);
		doThrow(new RejectException("Test MAIL from rejected")).when(messageHandler).from(anyString());

		final SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "localhost");

		try {
			client.from("john1@example.com");
			fail();
		} catch (final SMTPException e) {
			//expected to receive error
		}

		client.from("john2@example.com");
		client.quit();

		smtpServer.stop(); // wait for the server to catch up

		verify(messageHandlerFactory, times(2)).create(any(MessageContext.class));
		verify(messageHandler).from(anyString());
		verify(messageHandler).done();
		verify(messageHandler2).from(anyString());
		verify(messageHandler2).done();
		verifyNoMoreInteractions(messageHandlerFactory, messageHandler, messageHandler2);
	}
}
