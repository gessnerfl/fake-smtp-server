package de.gessnerfl.fakesmtp.server.smtp.server;

public class InvalidCommandNameException extends CommandException {
	public InvalidCommandNameException(final String string) {
		super(string);
	}
}
