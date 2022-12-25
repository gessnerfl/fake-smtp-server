package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

/**
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 */
public class VerifyCommand extends BaseCommand {
	public VerifyCommand() {
		super("VRFY", "The vrfy command.");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		sess.sendResponse("502 VRFY command is disabled");
	}
}
