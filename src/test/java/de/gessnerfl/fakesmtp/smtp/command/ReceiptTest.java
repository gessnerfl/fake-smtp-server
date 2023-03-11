package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;

class ReceiptTest extends AbstractCommandIntegrationTest {

	@Test
	void testReceiptBeforeMail() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("RCPT TO: bar@foo.com");
		this.expect("503 5.5.1 Error: need MAIL command");
	}

	@Test
	void testReceiptErrorInParams() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: success@example.com");
		this.expect("250 Ok");

		this.send("RCPT");
		this.expect("501 Syntax: RCPT TO: <address>  Error in parameters:");
	}

	@Test
	void testReceiptAccept() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: success@example.com");
		this.expect("250 Ok");

		this.send("RCPT TO: blocked@example.com");
		this.expect("553 <blocked@example.com> address unknown.");

		this.send("RCPT TO: success@example.com");
		this.expect("250 Ok");
	}

	@Test
	void testReceiptNoWhiteSpace() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: success@example.com");
		this.expect("250 Ok");

		this.send("RCPT TO:success@example.com");
		this.expect("250 Ok");
	}
}
