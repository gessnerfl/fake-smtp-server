package de.gessnerfl.fakesmtp.smtp;

import java.io.IOException;

public class TooMuchDataException extends IOException {
	public TooMuchDataException() {}

	public TooMuchDataException(final String message) {
		super(message);
	}
}