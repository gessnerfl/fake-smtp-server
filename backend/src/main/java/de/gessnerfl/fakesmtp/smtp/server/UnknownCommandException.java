package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.smtp.command.CommandException;

public class UnknownCommandException extends CommandException {
    public UnknownCommandException(final String string) {
        super(string);
    }
}
