package de.gessnerfl.fakesmtp.smtp;

public class RejectException extends RuntimeException {
	private final int code;

	public RejectException(final String message) {
		this(554, message);
	}

	public RejectException(final int code, final String message) {
		super(message);

		this.code = code;
	}

	public String getErrorResponse() {
		return this.code + " " + this.getMessage();
	}
}
