package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

public class QuitCommand extends BaseCommand {
	public QuitCommand() {
		super("QUIT", "Exit the SMTP session.");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.sendResponse("221 Bye");
		sess.quit();
	}
}