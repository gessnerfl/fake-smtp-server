package de.gessnerfl.fakesmtp.server.smtp.command;

import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.server.smtp.util.ServerTestCase;

class MailTest extends ServerTestCase {

	@Test
	void testMailNoHello() throws Exception {
		this.expect("220");

		this.send("MAIL FROM: test@example.com");
		this.expect("250");
	}

	@Test
	void testAlreadySpecified() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: test@example.com");
		this.expect("250 Ok");

		this.send("MAIL FROM: another@example.com");
		this.expect("503 5.5.1 Sender already specified.");
	}

	@Test
	void testInvalidSenders() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		// added <> because without them "lkjk" is a parameter
		// to the MAIL command. (Postfix responds accordingly)
		this.send("MAIL FROM: <test@lkjsd lkjk>");
		this.expect("553 <test@lkjsd lkjk> Invalid email address.");
	}

	@Test
	void testMalformedMailCommand() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL");
		this.expect("501 Syntax: MAIL FROM: <address>  Error in parameters:");
	}

	@Test
	void testEmptyFromCommand() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: <>");
		this.expect("250");
	}

	@Test
	void testEmptyEmailFromCommand() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM:");
		this.expect("501 Syntax: MAIL FROM: <address>");
	}

	@Test
	void testMailWithoutWhitespace() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM:<validuser@subethamail.org>");
		this.expect("250 Ok");
	}

	@Test
	void testSize() throws Exception {
		this.wiser.getServer().setMaxMessageSize(1000);
		this.expect("220");

		this.send("EHLO foo.com");
		this.expectContains("250-SIZE 1000");

		this.send("MAIL FROM:<validuser@subethamail.org> SIZE=100");
		this.expect("250 Ok");
	}

	@Test
	void testSizeWithoutSize() throws Exception {
		this.wiser.getServer().setMaxMessageSize(1000);
		this.expect("220");

		this.send("EHLO foo.com");
		this.expectContains("250-SIZE 1000");

		this.send("MAIL FROM:<validuser@subethamail.org>");
		this.expect("250 Ok");
	}

	@Test
	void testSizeTooLarge() throws Exception {
		this.wiser.getServer().setMaxMessageSize(1000);
		this.expect("220");

		this.send("EHLO foo.com");
		this.expectContains("250-SIZE 1000");

		this.send("MAIL FROM:<validuser@subethamail.org> SIZE=1001");
		this.expect("552");
	}
}
