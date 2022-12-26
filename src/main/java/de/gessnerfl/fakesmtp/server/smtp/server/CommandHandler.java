package de.gessnerfl.fakesmtp.server.smtp.server;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.gessnerfl.fakesmtp.server.smtp.DropConnectionException;

public class CommandHandler {
	private final static Logger log = LoggerFactory.getLogger(CommandHandler.class);

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

	/**
	 * Create a command handler with a specific set of commands.
	 *
	 * @param availableCommands the available commands (not null) TLS note: wrap
	 *                          commands with {@link RequireTLSCommandWrapper} when
	 *                          appropriate.
	 */
	public CommandHandler(final Collection<Command> availableCommands) {
		for (final Command command : availableCommands) {
			this.addCommand(command);
		}
	}

	/**
	 * Adds or replaces the specified command.
	 */
	public void addCommand(final Command command) {
		if (log.isDebugEnabled()) {
			log.debug("Added command: " + command.getName());
		}

		this.commandMap.put(command.getName(), command);
	}

	/**
	 * Returns the command object corresponding to the specified command name.
	 *
	 * @param commandName case insensitive name of the command.
	 * @return the command object, or null, if the command is unknown.
	 */
	public Command getCommand(final String commandName) {
		final String upperCaseCommandName = commandName.toUpperCase(Locale.ENGLISH);
		return this.commandMap.get(upperCaseCommandName);
	}

	public boolean containsCommand(final String command) {
		return this.commandMap.containsKey(command);
	}

	public Set<String> getVerbs() {
		return this.commandMap.keySet();
	}

	public void handleCommand(final Session context, final String commandString)
			throws SocketTimeoutException, IOException, DropConnectionException {
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
		Command command = null;
		final String key = this.toKey(commandString);
		if (key != null) {
			command = this.commandMap.get(key);
		}
		if (command == null) {
			// some commands have a verb longer than 4 letters
			final String verb = this.toVerb(commandString);
			if (verb != null) {
				command = this.commandMap.get(verb);
			}
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
