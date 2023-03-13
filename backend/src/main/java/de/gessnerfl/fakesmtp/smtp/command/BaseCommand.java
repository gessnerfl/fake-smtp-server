package de.gessnerfl.fakesmtp.smtp.command;

import de.gessnerfl.fakesmtp.smtp.server.HelpMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public abstract class BaseCommand implements Command {
	/** Name of the command, ie HELO */
	private final CommandVerb verb;

	/** The help message for this command */
	private final HelpMessage helpMsg;

	protected BaseCommand(final CommandVerb verb, final String help) {
		this.verb = verb;
		this.helpMsg = new HelpMessage(verb, help);
	}

	protected BaseCommand(final CommandVerb verb, final String help, final String argumentDescription) {
		this.verb = verb;
		this.helpMsg = new HelpMessage(verb, help, argumentDescription);
	}

	@Override
	public HelpMessage getHelp() {
		return this.helpMsg;
	}

	@Override
	public CommandVerb getVerb() {
		return this.verb;
	}

	protected String getArgPredicate(final String commandString) {
		if (commandString == null || commandString.length() < 4) {
			return "";
		}

		return commandString.substring(4).trim();
	}

	protected String[] getArgs(final String commandString) {
		final List<String> strings = new ArrayList<>();
		final StringTokenizer stringTokenizer = new StringTokenizer(commandString);
		while (stringTokenizer.hasMoreTokens()) {
			strings.add(stringTokenizer.nextToken());
		}

		return strings.toArray(new String[strings.size()]);
	}
}
