package de.gessnerfl.fakesmtp.smtp.command;

import de.gessnerfl.fakesmtp.smtp.server.InvalidCommandNameException;
import de.gessnerfl.fakesmtp.smtp.server.UnknownCommandException;

import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandRegistry {

    private final Map<CommandVerb, Command> commands;

    public CommandRegistry(final Command... commands) {
        this.commands = Stream.of(commands).collect(Collectors.toMap(Command::getVerb, Function.identity()));
    }

    public Command getCommandFromString(final String commandString) throws UnknownCommandException, InvalidCommandNameException {
        final var verb = getVerbByCommand(commandString);
        final var command = commands.get(verb);
        if (command == null) {
            throw new UnknownCommandException("Error: command not implemented");
        }
        return command;
    }

    private CommandVerb getVerbByCommand(final String commandString) throws InvalidCommandNameException, UnknownCommandException {
        if (commandString == null || commandString.length() < 4) {
            throw new InvalidCommandNameException("Error: bad syntax");
        }

        final var commandStringUpperCase = commandString.toUpperCase(Locale.ENGLISH);
        try {
            return CommandVerb.valueOf(commandStringUpperCase);
        } catch (IllegalArgumentException e1) {
            final StringTokenizer stringTokenizer = new StringTokenizer(commandStringUpperCase);
            try {
                return CommandVerb.valueOf(stringTokenizer.nextToken());
            } catch (IllegalArgumentException e2) {
                throw new UnknownCommandException("Error: command not implemented");
            }
        }
    }
}
