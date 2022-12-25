package de.gessnerfl.fakesmtp.server.smtp.auth;

/**
 * Exception expected to be thrown by a validator (i.e
 * UsernamePasswordValidator)
 *
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
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
