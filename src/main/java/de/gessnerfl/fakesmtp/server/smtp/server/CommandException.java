package de.gessnerfl.fakesmtp.server.smtp.server;

public class CommandException extends Exception {
	public CommandException(final String string, final Throwable throwable) {
		super(string, throwable);
	}

	public CommandException(final String string) {
		super(string);
	}

	public CommandException() {}

	public CommandException(final Throwable throwable) {
		super(throwable);
	}
}