package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;

class QuitTest extends AbstractCommandIntegrationTest {

	@Test
	void testQuit() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: test@example.com");
		this.expect("250 Ok");

		this.send("QUIT");
		this.expect("221 Bye");
	}
}
