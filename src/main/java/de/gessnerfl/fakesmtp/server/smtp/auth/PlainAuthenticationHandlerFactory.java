package de.gessnerfl.fakesmtp.server.smtp.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import de.gessnerfl.fakesmtp.server.smtp.util.Base64;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandler;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.RejectException;

/**
 * Implements the SMTP AUTH PLAIN mechanism.<br>
 * You are only required to plug your UsernamePasswordValidator implementation
 * for username and password validation to take effect.
 *
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 * @author Jeff Schnitzer
 * @author Ian White &lt;ibwhite@gmail.com&gt;
 */
public class PlainAuthenticationHandlerFactory implements AuthenticationHandlerFactory {
	static List<String> MECHANISMS = new ArrayList<>(1);
	static {
		MECHANISMS.add("PLAIN");
	}

	private final UsernamePasswordValidator helper;

	public PlainAuthenticationHandlerFactory(final UsernamePasswordValidator helper) {
		this.helper = helper;
	}

	@Override
	public List<String> getAuthenticationMechanisms() {
		return MECHANISMS;
	}

	@Override
	public AuthenticationHandler create() {
		return new Handler();
	}

	/**
	 */
	class Handler implements AuthenticationHandler {
		private String username;

		private String password;

		/* */
		@Override
		public String auth(final String clientInput) throws RejectException {
			final StringTokenizer stk = new StringTokenizer(clientInput);
			String secret = stk.nextToken();
			if (secret.trim().equalsIgnoreCase("AUTH")) {
				// Let's read the RFC2554 "initial-response" parameter
				// The line could be in the form of "AUTH PLAIN <base64Secret>"
				if (!stk.nextToken().trim().equalsIgnoreCase("PLAIN")) {
					// Mechanism mismatch
					throw new RejectException(504, "AUTH mechanism mismatch");
				}

				if (!stk.hasMoreTokens()) {
					// the client did not submit an initial response, we'll get it in the next pass
					return "334 Ok";
				}
				// the client submitted an initial response
				secret = stk.nextToken();
			}

			final byte[] decodedSecret = Base64.decode(secret);
			if (decodedSecret == null) {
				throw new RejectException(501, /* 5.5.4 */
						"Invalid command argument, not a valid Base64 string");
			}

			/*
			 * RFC4616: The client presents the authorization identity (identity to act as),
			 * followed by a NUL (U+0000) character, followed by the authentication identity
			 * (identity whose password will be used), followed by a NUL (U+0000) character,
			 * followed by the clear-text password.
			 */

			int i, j;
			for (i = 0; i < decodedSecret.length && decodedSecret[i] != 0; i++) {}
			if (i >= decodedSecret.length) {
				throw new RejectException(501, /* 5.5.4 */
						"Invalid command argument, does not contain NUL");
			}

			for (j = i + 1; j < decodedSecret.length && decodedSecret[j] != 0; j++) {}
			if (j >= decodedSecret.length) {
				throw new RejectException(501, /* 5.5.4 */
						"Invalid command argument, does not contain the second NUL");
			}

			@SuppressWarnings("unused")
			final String authorizationId = new String(decodedSecret, 0, i);
			final String authenticationId = new String(decodedSecret, i + 1, j - i - 1);
			final String passwd = new String(decodedSecret, j + 1, decodedSecret.length - j - 1);

			// might be nice to do something with authorizationId, but for
			// purposes of the UsernamePasswordValidator, we just want to use
			// authenticationId

			this.username = authenticationId;
			this.password = passwd;
			try {
				PlainAuthenticationHandlerFactory.this.helper.login(this.username.toString(), this.password);
			} catch (final LoginFailedException lfe) {
				throw new RejectException(535, /* 5.7.8 */
						"Authentication credentials invalid");
			}

			return null;
		}

		/* */
		@Override
		public Object getIdentity() {
			return this.username;
		}
	}
}
