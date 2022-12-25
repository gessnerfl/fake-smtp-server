package de.gessnerfl.fakesmtp.server.smtp.command;

import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.server.smtp.util.ServerTestCase;

/**
 * @author Jon Stevens
 */
public class CommandTest extends ServerTestCase {

	@Test
	public void testCommandHandling() throws Exception {
		this.expect("220");

		this.send("blah blah blah");
		this.expect("500 Error: command not implemented");
	}
}
