package de.gessnerfl.fakesmtp.server.smtp.util;

import de.gessnerfl.fakesmtp.server.smtp.Wiser;
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

	public static final int PORT = 2566;

	/**
	 * Override the accept method in Wiser so we can test the accept method().
	 */
	public static class TestWiser extends Wiser {
		@Override
		public boolean accept(final String from, final String recipient) {
			return !recipient.equals("failure@subethamail.org");
		}
	}

	protected TestWiser wiser;

	protected Client c;

	@BeforeEach
	protected void setUp() throws Exception {
		this.wiser = createTestWiser();
		this.wiser.start();
		this.c = new Client("localhost", PORT);
	}

	protected TestWiser createTestWiser() {
		var wiser = new TestWiser();
		wiser.setHostname("localhost");
		wiser.setPort(PORT);
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
