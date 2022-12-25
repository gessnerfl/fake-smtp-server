package de.gessnerfl.fakesmtp.server.smtp.server;

/**
 * @author Ian McFarland &lt;ian@neo.com&gt;
 */
@SuppressWarnings("serial")
public class InvalidCommandNameException extends CommandException {
	public InvalidCommandNameException() {}

	public InvalidCommandNameException(final String string) {
		super(string);
	}

	public InvalidCommandNameException(final String string, final Throwable throwable) {
		super(string, throwable);
	}

	public InvalidCommandNameException(final Throwable throwable) {
		super(throwable);
	}
}
