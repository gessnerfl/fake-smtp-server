package de.gessnerfl.fakesmtp.smtp;

public class DropConnectionException extends RejectException {
	public DropConnectionException() {}

	public DropConnectionException(final String message) {
		super(message);
	}

	public DropConnectionException(final int code, final String message) {
		super(code, message);
	}
}
