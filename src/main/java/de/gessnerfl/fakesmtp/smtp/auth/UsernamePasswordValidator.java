package de.gessnerfl.fakesmtp.smtp.auth;

/**
 * Use this when your authentication scheme uses a username and a password.
 */
public interface UsernamePasswordValidator {
	void login(final String username, final String password) throws LoginFailedException;
}
