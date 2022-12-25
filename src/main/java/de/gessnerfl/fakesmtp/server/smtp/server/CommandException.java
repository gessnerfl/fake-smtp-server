package de.gessnerfl.fakesmtp.server.smtp.server;

/**
 * @author Ian McFarland &lt;ian@neo.com&gt;
 */
@SuppressWarnings("serial")
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
