package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.smtp.server.Session;

public class VerifyCommand extends BaseCommand {
	public VerifyCommand() {
		super("VRFY", "The vrfy command.");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.sendResponse("502 VRFY command is disabled");
	}
}
