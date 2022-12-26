package de.gessnerfl.fakesmtp.server.smtp.auth;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandler;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.RejectException;
import org.springframework.util.Base64Utils;

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
    private final static List<String> MECHANISMS = Collections.singletonList("PLAIN");

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
     *
     */
    class Handler implements AuthenticationHandler {
        private String username;

        /* */
        @Override
        public String auth(final String clientInput) throws RejectException {
            final StringTokenizer stk = new StringTokenizer(clientInput);
            String secret = stk.nextToken();
            if (secret.trim().equalsIgnoreCase("AUTH")) {
                // Let's read the RFC2554 "initial-response" parameter
                // The line could be in the form of "AUTH PLAIN <base64Secret>"
                if (!stk.nextToken().trim().equalsIgnoreCase("PLAIN")) {
                    throw new RejectException(504, "AUTH mechanism mismatch");
                }

                if (!stk.hasMoreTokens()) {
                    // the client did not submit an initial response, we'll get it in the next pass
                    return "334 Ok";
                }
                // the client submitted an initial response
                secret = stk.nextToken();
            }

            final byte[] decodedSecret = Base64Utils.decodeFromString(secret);
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
            int firstNul;
            for (firstNul = 0; firstNul < decodedSecret.length && decodedSecret[firstNul] != 0; firstNul++) {
                //Nothing to do; loop required to find first NUL character in decoded secret
            }
            if (firstNul >= decodedSecret.length) {
                throw new RejectException(501, "Invalid command argument, does not contain NUL");
            }

            int secondNul;
            for (secondNul = firstNul + 1; secondNul < decodedSecret.length && decodedSecret[secondNul] != 0; secondNul++) {
                //Nothing to do; loop required to find second NUL character in decoded secret
            }
            if (secondNul >= decodedSecret.length) {
                throw new RejectException(501, "Invalid command argument, does not contain the second NUL");
            }

            @SuppressWarnings("unused") final var authorizationId = new String(decodedSecret, 0, firstNul);
            final var authenticationId = new String(decodedSecret, firstNul + 1, secondNul - firstNul - 1);
            final var password = new String(decodedSecret, secondNul + 1, decodedSecret.length - secondNul - 1);

            // might be nice to do something with authorizationId, but for
            // purposes of the UsernamePasswordValidator, we just want to use
            // authenticationId

            this.username = authenticationId;
            try {
                PlainAuthenticationHandlerFactory.this.helper.login(this.username, password);
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
