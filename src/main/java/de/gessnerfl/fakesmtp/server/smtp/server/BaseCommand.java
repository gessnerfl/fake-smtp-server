package de.gessnerfl.fakesmtp.server.smtp.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.gessnerfl.fakesmtp.server.smtp.DropConnectionException;

public abstract class BaseCommand implements Command {
	private static final Logger log = LoggerFactory.getLogger(BaseCommand.class);

	/** Name of the command, ie HELO */
	private final String name;

	/** The help message for this command */
	private final HelpMessage helpMsg;

	protected BaseCommand(final String name, final String help) {
		this.name = name;
		this.helpMsg = new HelpMessage(name, help);
	}

	protected BaseCommand(final String name, final String help, final String argumentDescription) {
		this.name = name;
		this.helpMsg = new HelpMessage(name, help, argumentDescription);
	}

	@Override
	public HelpMessage getHelp() {
		return this.helpMsg;
	}

	@Override
	public String getName() {
		return this.name;
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
