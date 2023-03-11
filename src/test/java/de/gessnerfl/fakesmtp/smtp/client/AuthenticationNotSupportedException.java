package de.gessnerfl.fakesmtp.smtp.client;

import java.io.IOException;

/**
 * Indicates that the server either does not support authentication at all or no
 * authentication mechanism exists which is supported by both the server and the
 * client.
 */
public class AuthenticationNotSupportedException extends IOException {
	private static final long serialVersionUID = 4269158574227243089L;

	public AuthenticationNotSupportedException(final String message) {
		super(message);
	}
}
