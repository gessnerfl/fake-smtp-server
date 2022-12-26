package de.gessnerfl.fakesmtp.server.smtp.command;

import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.server.smtp.util.ServerTestCase;

public class DataTest extends ServerTestCase {

	@Test
	void testNeedMail() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("DATA");
		this.expect("503 5.5.1 Error: need MAIL command");
	}

	@Test
	void testNeedRcpt() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: success@subethamail.org");
		this.expect("250");

		this.send("DATA");
		this.expect("503 Error: need RCPT command");
	}

	@Test
	void testData() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: success@subethamail.org");
		this.expect("250");

		this.send("RCPT TO: success@subethamail.org");
		this.expect("250");

		this.send("DATA");
		this.expect("354 End data with <CR><LF>.<CR><LF>");
	}

	@Test
	void testRsetAfterData() throws Exception {
		this.expect("220");

		this.send("HELO foo.com");
		this.expect("250");

		this.send("MAIL FROM: success@subethamail.org");
		this.expect("250");

		this.send("RCPT TO: success@subethamail.org");
		this.expect("250");

		this.send("DATA");
		this.expect("354 End data with <CR><LF>.<CR><LF>");

		this.send("alsdkfj \r\n.");

		this.send("RSET");
		this.expect("250 Ok");

		this.send("HELO foo.com");
		this.expect("250");
	}
}
