package de.gessnerfl.fakesmtp.server.smtp;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.SocketException;

import de.gessnerfl.fakesmtp.server.smtp.client.SMTPClient;

/**
 * This class tests connection timeouts.
 *
 * @author Jeff Schnitzer
 */
public class TimeoutTest {
	public static final int PORT = 2566;

	// @Test
	public void testTimeout() throws Exception {
		final Wiser wiser = new Wiser();
		wiser.setPort(PORT);
		wiser.getServer().setConnectionTimeout(1000);
		wiser.start();

		final SMTPClient client = new SMTPClient("localhost", PORT);
		client.sendReceive("HELO foo");
		Thread.sleep(2000);
		try {
			client.sendReceive("HELO bar");
			fail();
		} catch (final SocketException e) {
			// expected
		} finally {
			wiser.stop();
		}
	}
}
