package de.gessnerfl.fakesmtp.smtp;

import java.util.Properties;

import jakarta.mail.Session;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class attempts to quickly start/stop 10 Wiser servers. It makes sure
 * that the socket bind address is correctly shut down.
 */
class StartStopTest {
	private Session session;
	private int randomPort;

	protected int counter = 0;

	@BeforeEach
	void setUp() {
		randomPort = RandomUtils.nextInt(1024,65536);
		final Properties props = new Properties();
		props.setProperty("mail.smtp.host", "localhost");
		props.setProperty("mail.smtp.port", Integer.toString(randomPort));
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
		wiser.setPort(randomPort);

		wiser.start();

		if (pause) {
			Thread.sleep(1000);
		}

		wiser.stop();

		this.counter++;
	}
}
