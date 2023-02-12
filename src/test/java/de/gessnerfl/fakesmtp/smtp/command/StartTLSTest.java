package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;

class StartTLSTest extends AbstractCommandIntegrationTest {
	@Test
	void testQuit() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("STARTTLS foo");
		this.expect("501 Syntax error (no parameters allowed)");

		this.send("QUIT");
		this.expect("221 Bye");
	}
}
