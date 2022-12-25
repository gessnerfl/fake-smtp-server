package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

/**
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Jeff Schnitzer
 * @author Scott Hernandez
 */
public class HelloCommand extends BaseCommand {
	public HelloCommand() {
		super("HELO", "Introduce yourself.", "<hostname>");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		final String[] args = this.getArgs(commandString);
		if (args.length < 2) {
			sess.sendResponse("501 Syntax: HELO <hostname>");
			return;
		}

		sess.resetMailTransaction();
		sess.setHelo(args[1]);

		sess.sendResponse("250 " + sess.getServer().getHostName());
	}
}
