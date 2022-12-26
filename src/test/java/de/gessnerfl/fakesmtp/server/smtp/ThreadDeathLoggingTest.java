package de.gessnerfl.fakesmtp.server.smtp;

import java.io.IOException;
import java.io.InputStream;

import de.gessnerfl.fakesmtp.server.smtp.client.SMTPException;
import de.gessnerfl.fakesmtp.server.smtp.client.SmartClient;
import de.gessnerfl.fakesmtp.server.smtp.helper.SimpleMessageListener;
import de.gessnerfl.fakesmtp.server.smtp.helper.SimpleMessageListenerAdapter;
import de.gessnerfl.fakesmtp.server.smtp.server.SMTPServer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ThreadDeathLoggingTest {
	/**
	 * This test can be used to check if an Error or RuntimeException actually
	 * logged, but it requires manual running. For example remove mail.jar from the
	 * classpath, and check the error log if it contains logged NoClassDef
	 * exception. See also the comment within the function how to check for a NPE.
	 * Note that any exception that causes a thread death is printed on stderr by
	 * the default uncaughtExceptionHandler of the JRE, but this is not what you are
	 * looking for.
	 *
	 * @throws SMTPException on SMTP error
	 * @throws IOException   on IO error
	 */
	@Disabled("Requires manual setup and verification")
	@Test()
	void testNoMailJar() throws SMTPException, IOException {
		// if this variable is set to null, than a NPE will be thrown, which is
		// also good for testing.
		final MessageHandlerFactory handlerFactory = new SimpleMessageListenerAdapter(new SimpleMessageListener() {
			@Override
			public void deliver(final String from, final String recipient, final InputStream data)
					throws TooMuchDataException, IOException {}

			@Override
			public boolean accept(final String from, final String recipient) {
				return false;
			}
		});
		final SMTPServer smtpServer = new SMTPServer(handlerFactory);
		smtpServer.setPort(0);
		smtpServer.start();
		try {
			final SmartClient client = new SmartClient("localhost", smtpServer.getPort(), "test-client.example.org");
			client.from("john@exmaple.com");
			client.to("jane@example.org");
			client.quit();
		} finally {
			smtpServer.stop();
		}
	}
}
