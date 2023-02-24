package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import de.gessnerfl.fakesmtp.smtp.server.HelpMessage;
import de.gessnerfl.fakesmtp.smtp.server.InvalidCommandNameException;
import de.gessnerfl.fakesmtp.smtp.server.Session;
import de.gessnerfl.fakesmtp.smtp.server.UnknownCommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);

	/**
	 * The map of known SMTP commands. Keys are upper case names of the commands.
	 */
	private final Map<String, Command> commandMap = new HashMap<>();

	public CommandHandler() {
		// This solution should be more robust than the earlier "manual" configuration.
		for (final CommandRegistry registry : CommandRegistry.values()) {
			this.addCommand(registry.getCommand());
		}
	}

	private void addCommand(final Command command) {
		LOGGER.debug("Added command: {}", command.getName());
		this.commandMap.put(command.getName(), command);
	}

	public Set<String> getVerbs() {
		return this.commandMap.keySet();
	}

	public void handleCommand(final Session context, final String commandString) throws IOException {
		try {
			final Command command = this.getCommandFromString(commandString);
			command.execute(commandString, context);
		} catch (final CommandException e) {
			context.sendResponse("500 " + e.getMessage());
		}
	}

	/**
	 * @return the HelpMessage object for the given command name (verb)
	 * @throws CommandException on command error
	 */
	public HelpMessage getHelp(final String command) throws CommandException {
		return this.getCommandFromString(command).getHelp();
	}

	private Command getCommandFromString(final String commandString)
			throws UnknownCommandException, InvalidCommandNameException {
		final String key = this.toKey(commandString);
		var command = this.commandMap.get(key);
		if (command == null) {
			// some commands have a verb longer than 4 letters
			final String verb = this.toVerb(commandString);
			command = this.commandMap.get(verb);
		}
		if (command == null) {
			throw new UnknownCommandException("Error: command not implemented");
		}
		return command;
	}

	private String toKey(final String string) throws InvalidCommandNameException {
		if (string == null || string.length() < 4) {
			throw new InvalidCommandNameException("Error: bad syntax");
		}

		return string.substring(0, 4).toUpperCase(Locale.ENGLISH);
	}

	private String toVerb(final String string) throws InvalidCommandNameException {
		final StringTokenizer stringTokenizer = new StringTokenizer(string);
		if (!stringTokenizer.hasMoreTokens()) {
			throw new InvalidCommandNameException("Error: bad syntax");
		}

		return stringTokenizer.nextToken().toUpperCase(Locale.ENGLISH);
	}
}
