package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;

class CommandTest extends AbstractCommandIntegrationTest {

	@Test
	void testCommandHandling() throws Exception {
		this.expect("220");

		this.send("blah blah blah");
		this.expect("500 Error: command not implemented");
	}
}
