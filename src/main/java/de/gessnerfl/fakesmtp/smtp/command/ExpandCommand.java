package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.smtp.server.Session;

public class ExpandCommand extends BaseCommand {
	public ExpandCommand() {
		super("EXPN", "The expn command.");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.sendResponse("502 EXPN command is disabled");
	}
}
