package de.gessnerfl.fakesmtp.server.smtp.command;

import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.server.smtp.util.ServerTestCase;

public class StartTLSTest extends ServerTestCase {
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
