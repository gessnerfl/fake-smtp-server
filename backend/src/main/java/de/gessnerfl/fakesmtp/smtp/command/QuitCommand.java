package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.smtp.server.Session;

public class QuitCommand extends BaseCommand {
	public QuitCommand() {
		super(CommandVerb.QUIT, "Exit the SMTP session.");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.sendResponse("221 Bye");
		sess.quit();
	}
}
