package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.smtp.util.ServerTestCase;

class CommandTest extends ServerTestCase {

	@Test
	void testCommandHandling() throws Exception {
		this.expect("220");

		this.send("blah blah blah");
		this.expect("500 Error: command not implemented");
	}
}
