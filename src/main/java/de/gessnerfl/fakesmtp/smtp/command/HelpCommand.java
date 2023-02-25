package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.smtp.server.BaseSmtpServer;
import de.gessnerfl.fakesmtp.smtp.server.Session;

/**
 * Provides a help topic system for people to interact with.
 */
public class HelpCommand extends BaseCommand {
	public HelpCommand() {
		super(CommandVerb.HELP,
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

	private String getCommandMessage(final BaseSmtpServer server) {
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

	protected String getFormattedTopicList(final BaseSmtpServer server) {
		final StringBuilder sb = new StringBuilder();
		for (final CommandVerb key : CommandVerb.values()) {
			sb.append("214-     ").append(key.name()).append("\r\n");
		}
		return sb.toString();
	}
}
