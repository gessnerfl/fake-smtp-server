package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;

import de.gessnerfl.fakesmtp.smtp.server.HelpMessage;
import de.gessnerfl.fakesmtp.smtp.server.Session;

public class CommandHandler {
    private final CommandRegistry commandRegistry;

    public CommandHandler(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public void handleCommand(final Session context, final String commandString) throws IOException {
        try {
            final Command command = commandRegistry.getCommandFromString(commandString);
            command.execute(commandString, context);
        } catch (final CommandException e) {
            context.sendResponse("500 " + e.getMessage());
        }
    }

    public HelpMessage getHelp(final String command) throws CommandException {
        return commandRegistry.getCommandFromString(command).getHelp();
    }
}
