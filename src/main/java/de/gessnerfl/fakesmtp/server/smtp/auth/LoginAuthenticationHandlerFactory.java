package de.gessnerfl.fakesmtp.server.smtp.auth;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandler;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.RejectException;
import org.springframework.util.Base64Utils;

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
 * @author Marco Trevisan &lt;mrctrevisan@yahoo.it&gt;
 * @author Jeff Schnitzer
 * @see <a href="http://tools.ietf.org/html/draft-murchison-sasl-login-00">The
 * LOGIN SASL Mechanism</a>
 * @see <a href=
 * "http://download.microsoft.com/download/5/d/d/5dd33fdf-91f5-496d-9884-0a0b0ee698bb/%5BMS-XLOGIN%5D.pdf">[MS-XLOGIN]</a>
 */
public class LoginAuthenticationHandlerFactory implements AuthenticationHandlerFactory {
    private static final List<String> MECHANISMS = Collections.singletonList("LOGIN");
    private static final String INVALID_COMMAND_ARGUMENT_NOT_A_VALID_BASE_64_STRING = "Invalid command argument, not a valid Base64 string";
    private static final byte[] USERNAME_ASCII_BYTES = "Username:".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PASSWORD_ASCII_BYTES = "Password:".getBytes(StandardCharsets.US_ASCII);

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
     *
     */
    class Handler implements AuthenticationHandler {
        private String username;

        @Override
        public String auth(final String clientInput) throws RejectException {
            final var stk = new StringTokenizer(clientInput);
            final var token = stk.nextToken();
            if (token.trim().equalsIgnoreCase("AUTH")) {
                if (!stk.nextToken().trim().equalsIgnoreCase("LOGIN")) {
                    throw new RejectException(504, "AUTH mechanism mismatch");
                }

                if (!stk.hasMoreTokens()) {
                    return "334 " + Base64Utils.encodeToString(USERNAME_ASCII_BYTES);
                }
                // The client submitted an initial response, which should be
                // the username.
                // .Net's built in System.Net.Mail.SmtpClient sends its
                // authentication this way (and this way only).
                final var decoded = Base64Utils.decodeFromString(stk.nextToken());
                username = new String(decoded, StandardCharsets.UTF_8);
                return "334 " + Base64Utils.encodeToString(PASSWORD_ASCII_BYTES);
            }

            if (this.username == null) {
                final byte[] decoded = Base64Utils.decodeFromString(clientInput);
                this.username = new String(decoded, StandardCharsets.UTF_8);
                return "334 " + Base64Utils.encodeToString(PASSWORD_ASCII_BYTES);
            }

            final var decoded = Base64Utils.decodeFromString(clientInput);
            final var password = new String(decoded, StandardCharsets.UTF_8);
            try {
                LoginAuthenticationHandlerFactory.this.helper.login(this.username, password);
            } catch (final LoginFailedException lfe) {
                throw new RejectException(535, "Authentication credentials invalid");
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
