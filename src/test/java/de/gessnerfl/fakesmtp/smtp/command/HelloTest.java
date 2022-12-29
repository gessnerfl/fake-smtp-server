package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.smtp.util.ServerTestCase;

class HelloTest extends ServerTestCase {

	@Test
	void testHelloCommand() throws Exception {
		this.expect("220");

		this.send("HELO");
		this.expect("501 Syntax: HELO <hostname>");

		this.send("HELO");
		this.expect("501 Syntax: HELO <hostname>");

		// Correct!
		this.send("HELO foo.com");
		this.expect("250");

		// Correct!
		this.send("HELO foo.com");
		this.expect("250");
	}

	@Test
	void testHelloReset() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: test@foo.com");
		this.expect("250 Ok");

		this.send("RSET");
		this.expect("250 Ok");

		this.send("MAIL FROM: test@foo.com");
		this.expect("250 Ok");
	}

	@Test
	void testEhloSize() throws Exception {
		this.wiser.getServer().setMaxMessageSize(1000);
		this.expect("220");

		this.send("EHLO foo.com");
		this.expectContains("250-SIZE 1000");
	}
}
