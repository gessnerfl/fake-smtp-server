package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.smtp.command.CommandException;

public class InvalidCommandNameException extends CommandException {
	public InvalidCommandNameException(final String string) {
		super(string);
	}
}
