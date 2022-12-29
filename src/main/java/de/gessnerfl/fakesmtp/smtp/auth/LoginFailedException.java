package de.gessnerfl.fakesmtp.smtp.auth;

/**
 * Exception expected to be thrown by a validator (i.e
 * UsernamePasswordValidator)
 */
@SuppressWarnings("serial")
public class LoginFailedException extends Exception {
	/** Creates a new instance of LoginFailedException */
	public LoginFailedException() {
		super("Login failed.");
	}

	/** Creates a new instance of LoginFailedException */
	public LoginFailedException(final String msg) {
		super(msg);
	}
}
