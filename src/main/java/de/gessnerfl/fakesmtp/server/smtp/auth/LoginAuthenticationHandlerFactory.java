package de.gessnerfl.fakesmtp.server.smtp.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import de.gessnerfl.fakesmtp.server.smtp.util.Base64;
import de.gessnerfl.fakesmtp.server.smtp.util.TextUtils;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandler;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.RejectException;

/**
 * Implements the SMTP AUTH LOGIN mechanism.<br>
 * You are only required to plug your UsernamePasswordValidator implementation
 * for username and password validation to take effect.
 * <p>
 * LOGIN is an obsolete authentication method which has no formal specification.
 * There is an expired IETF draft for informational purposes. A Microsoft
 * document can also be found, which intends to specify the LOGIN mechanism. The
 * latter is not entirely compatible, neither with the IETF draft nor with RFC
 * 4954 (SMTP Service Extension for Authentication). However this implementation
 * is likely usable with clients following any of the two documents.
 *
 * @see <a href="http://tools.ietf.org/html/draft-murchison-sasl-login-00">The
 *      LOGIN SASL Mechanism</a>
 * @see <a href=
 *      "http://download.microsoft.com/download/5/d/d/5dd33fdf-91f5-496d-9884-0a0b0ee698bb/%5BMS-XLOGIN%5D.pdf">[MS-XLOGIN]</a>
 *
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 * @author Jeff Schnitzer
 */
public class LoginAuthenticationHandlerFactory implements AuthenticationHandlerFactory {
	static List<String> MECHANISMS = new ArrayList<>(1);
	static {
		MECHANISMS.add("LOGIN");
	}

	private final UsernamePasswordValidator helper;

	public LoginAuthenticationHandlerFactory(final UsernamePasswordValidator helper) {
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

		@Override
		public String auth(final String clientInput) throws RejectException {
			final StringTokenizer stk = new StringTokenizer(clientInput);
			final String token = stk.nextToken();
			if (token.trim().equalsIgnoreCase("AUTH")) {
				if (!stk.nextToken().trim().equalsIgnoreCase("LOGIN")) {
					// Mechanism mismatch
					throw new RejectException(504, "AUTH mechanism mismatch");
				}

				if (!stk.hasMoreTokens()) {
					return "334 " + Base64.encodeToString(TextUtils.getAsciiBytes("Username:"), false);
				}
				// The client submitted an initial response, which should be
				// the username.
				// .Net's built in System.Net.Mail.SmtpClient sends its
				// authentication this way (and this way only).
				final byte[] decoded = Base64.decode(stk.nextToken());
				if (decoded == null) {
					throw new RejectException(501, /* 5.5.4 */
							"Invalid command argument, not a valid Base64 string");
				}
				username = TextUtils.getStringUtf8(decoded);

				return "334 " + Base64.encodeToString(TextUtils.getAsciiBytes("Password:"), false);
			}

			if (this.username == null) {
				final byte[] decoded = Base64.decode(clientInput);
				if (decoded == null) {
					throw new RejectException(501, /* 5.5.4 */
							"Invalid command argument, not a valid Base64 string");
				}

				this.username = TextUtils.getStringUtf8(decoded);

				return "334 " + Base64.encodeToString(TextUtils.getAsciiBytes("Password:"), false);
			}

			final byte[] decoded = Base64.decode(clientInput);
			if (decoded == null) {
				throw new RejectException(501, /* 5.5.4 */
						"Invalid command argument, not a valid Base64 string");
			}

			this.password = TextUtils.getStringUtf8(decoded);
			try {
				LoginAuthenticationHandlerFactory.this.helper.login(this.username, this.password);
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
