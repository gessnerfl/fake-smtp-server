package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

/**
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Jeff Schnitzer
 */
public class NoopCommand extends BaseCommand {
	public NoopCommand() {
		super("NOOP", "The noop command");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.sendResponse("250 Ok");
	}
}
