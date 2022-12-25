package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.CommandException;
import de.gessnerfl.fakesmtp.server.smtp.server.SMTPServer;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

/**
 * Provides a help topic system for people to interact with.
 *
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Scott Hernandez
 */
public class HelpCommand extends BaseCommand {
	public HelpCommand() {
		super("HELP",
				"The HELP command gives help info about the topic specified.\n"
						+ "For a list of topics, type HELP by itself.",
				"[ <topic> ]");
	}

	@Override
	public void execute(final String commandString, final Session context) throws IOException {
		final String args = this.getArgPredicate(commandString);
		if ("".equals(args)) {
			context.sendResponse(this.getCommandMessage(context.getServer()));
			return;
		}
		try {
			context.sendResponse(context.getServer().getCommandHandler().getHelp(args).toOutputString());
		} catch (final CommandException e) {
			context.sendResponse("504 HELP topic \"" + args + "\" unknown.");
		}
	}

	private String getCommandMessage(final SMTPServer server) {
		return "214-"
				+ server.getSoftwareName()
				+ " on "
				+ server.getHostName()
				+ "\r\n"
				+ "214-Topics:\r\n"
				+ this.getFormattedTopicList(server)
				+ "214-For more info use \"HELP <topic>\".\r\n"
				+ "214 End of HELP info";
	}

	protected String getFormattedTopicList(final SMTPServer server) {
		final StringBuilder sb = new StringBuilder();
		for (final String key : server.getCommandHandler().getVerbs()) {
			sb.append("214-     " + key + "\r\n");
		}
		return sb.toString();
	}
}
