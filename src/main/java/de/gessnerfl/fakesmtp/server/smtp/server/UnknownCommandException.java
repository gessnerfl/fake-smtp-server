package de.gessnerfl.fakesmtp.server.smtp.server;

/**
 * @author Ian McFarland &lt;ian@neo.com&gt;
 */
@SuppressWarnings("serial")
public class UnknownCommandException extends CommandException {
	public UnknownCommandException() {}

	public UnknownCommandException(final String string) {
		super(string);
	}

	public UnknownCommandException(final String string, final Throwable throwable) {
		super(string, throwable);
	}

	public UnknownCommandException(final Throwable throwable) {
		super(throwable);
	}
}
