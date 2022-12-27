package de.gessnerfl.fakesmtp.server.smtp;

import java.time.Duration;
import java.util.Properties;

import jakarta.mail.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class attempts to quickly start/stop 10 Wiser servers. It makes sure
 * that the socket bind address is correctly shut down.
 */
class StartStopTest {
	public static final int PORT = 2566;

	protected Session session;

	protected int counter = 0;

	@BeforeEach
	void setUp() {
		final Properties props = new Properties();
		props.setProperty("mail.smtp.host", "localhost");
		props.setProperty("mail.smtp.port", Integer.toString(PORT));
		this.session = Session.getDefaultInstance(props);
	}

	@Test
	void testMultipleStartStop() throws Exception {
		for (int i = 0; i < 10; i++) {
			this.startStop(i > 5);
		}
		assertEquals(10, this.counter);
	}

	private void startStop(final boolean pause) throws Exception {
		final Wiser wiser = new Wiser();
		wiser.setPort(PORT);

		wiser.start();

		if (pause) {
			Duration.ofSeconds(1).wait();
		}

		wiser.stop();

		this.counter++;
	}
}
