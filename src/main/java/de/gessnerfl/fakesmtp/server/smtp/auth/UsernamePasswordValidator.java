package de.gessnerfl.fakesmtp.server.smtp.auth;

/**
 * Use this when your authentication scheme uses a username and a password.
 *
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 */
public interface UsernamePasswordValidator {
	void login(final String username, final String password) throws LoginFailedException;
}
