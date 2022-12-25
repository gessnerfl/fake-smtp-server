package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

/**
 * @author Michele Zuccala &lt;zuccala.m@gmail.com&gt;
 */
public class ExpandCommand extends BaseCommand {
	public ExpandCommand() {
		super("EXPN", "The expn command.");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.sendResponse("502 EXPN command is disabled");
	}
}
