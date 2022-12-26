package de.gessnerfl.fakesmtp.server.smtp.server;

public class UnknownCommandException extends CommandException {
    public UnknownCommandException(final String string) {
        super(string);
    }
}
