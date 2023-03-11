package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.smtp.server.Session;

public class ResetCommand extends BaseCommand {
	public ResetCommand() {
		super(CommandVerb.RSET, "Resets the system.");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.resetMailTransaction();

		sess.sendResponse("250 Ok");
	}
}
