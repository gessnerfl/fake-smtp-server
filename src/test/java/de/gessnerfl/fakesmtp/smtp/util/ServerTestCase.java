package de.gessnerfl.fakesmtp.smtp.util;

import de.gessnerfl.fakesmtp.smtp.Wiser;
import de.gessnerfl.fakesmtp.smtp.client.Client;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class for testing the SMTP server at the raw protocol level. Handles
 * setting up and tearing down of the server.
 */
public abstract class ServerTestCase {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(ServerTestCase.class);

	/**
	 * Override the accept method in Wiser so we can test the accept method().
	 */
	public static class TestWiser extends Wiser {
		@Override
		public boolean accept(final String from, final String recipient) {
			return !recipient.equals("failure@example.com");
		}
	}

	protected TestWiser wiser;

	protected Client c;

	@BeforeEach
	protected void setUp() throws Exception {
		int randomPort = RandomUtils.nextInt(1024,65536);
		this.wiser = createTestWiser(randomPort);
		this.wiser.start();
		this.c = new Client("localhost", randomPort);
	}

	protected TestWiser createTestWiser(int serverPort) {
		var wiser = new TestWiser();
		wiser.setHostname("localhost");
		wiser.setPort(serverPort);
		return wiser;
	}

	@AfterEach
	protected void tearDown() throws Exception {
		this.wiser.stop();
		this.wiser = null;

		this.c.close();
	}

	public void send(final String msg) throws Exception {
		this.c.send(msg);
	}

	public void expect(final String msg) throws Exception {
		this.c.expect(msg);
	}

	public void expectContains(final String msg) throws Exception {
		this.c.expectContains(msg);
	}
}
