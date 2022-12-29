package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.smtp.server.Session;

public class NoopCommand extends BaseCommand {
	public NoopCommand() {
		super("NOOP", "The noop command");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.sendResponse("250 Ok");
	}
}
