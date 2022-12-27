package de.gessnerfl.fakesmtp.server.smtp.command;

import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.server.smtp.util.ServerTestCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

	@ParameterizedTest
	@CsvSource({
			"MAIL FROM: <test@lkjsd lkjk>, 553 <test@lkjsd lkjk> Invalid email address.",
			"MAIL, 501 Syntax: MAIL FROM: <address>  Error in parameters:",
			"MAIL FROM: <>, 250",
			"MAIL FROM:, 501 Syntax: MAIL FROM: <address>",
			"MAIL FROM:<validuser@subethamail.org>, 250 Ok"
	})
	void testMailCommands(final String command, final String expectedResponse) throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		// added <> because without them "lkjk" is a parameter
		// to the MAIL command. (Postfix responds accordingly)
		this.send(command);
		this.expect(expectedResponse);
	}

	@ParameterizedTest
	@CsvSource({
			"MAIL FROM:<validuser@subethamail.org> SIZE=100, 250 Ok",
			"MAIL FROM:<validuser@subethamail.org>, 250 Ok",
			"MAIL FROM:<validuser@subethamail.org> SIZE=1001, 552"
	})
	void testSizes(final String command, final String expectedResponse) throws Exception {
		this.wiser.getServer().setMaxMessageSize(1000);
		this.expect("220");

		this.send("EHLO foo.com");
		this.expectContains("250-SIZE 1000");

		this.send(command);
		this.expect(expectedResponse);
	}
}
