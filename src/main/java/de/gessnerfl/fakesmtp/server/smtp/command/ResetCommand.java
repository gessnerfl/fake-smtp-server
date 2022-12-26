package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

public class ResetCommand extends BaseCommand {
	public ResetCommand() {
		super("RSET", "Resets the system.");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.resetMailTransaction();

		sess.sendResponse("250 Ok");
	}
}
