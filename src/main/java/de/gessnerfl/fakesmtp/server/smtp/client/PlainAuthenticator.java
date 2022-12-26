package de.gessnerfl.fakesmtp.server.smtp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.gessnerfl.fakesmtp.server.smtp.util.Base64;

/**
 * PlainAuthenticator implements the SASL PLAIN mechanism which authenticates
 * the client using a name - password combination.
 *
 * @see <a href="http://tools.ietf.org/html/rfc4616">RFC 4616: The PLAIN Simple
 * Authentication and Security Layer (SASL) Mechanism</a>
 */
public class PlainAuthenticator implements Authenticator {
    private final String user;

    private final String password;

    private final SmartClient smartClient;

    public PlainAuthenticator(final SmartClient smartClient, final String user, final String password) {
        this.smartClient = smartClient;
        this.user = user;
        this.password = password;
    }

    @Override
    public void authenticate() throws IOException {
        checkAuthPlainSupport();

        final String initialClientResponse = constructInitialClientResponse();
        smartClient.sendAndCheck("AUTH PLAIN " + initialClientResponse);
    }

    /**
     * Checks if the server supports this mechanism.
     *
     * @throws AuthenticationNotSupportedException if the server does not support
     *                                             this mechanism or authentication
     *                                             at all.
     */
    private void checkAuthPlainSupport() throws AuthenticationNotSupportedException {
        final String mechanismsString = smartClient.getExtensions().get("AUTH");
        if (mechanismsString == null) {
            throw new AuthenticationNotSupportedException("Cannot authenticate, because the AUTH extension is "
                    + "not supported by the server. Maybe the server expects "
                    + "TLS first");
        }
        final Set<String> mechanisms = parseMechanismsList(mechanismsString);
        if (!mechanisms.contains("PLAIN")) {
            throw new AuthenticationNotSupportedException("Cannot authenticate, because the PLAIN mechanism is "
                    + "not supported by the server. Maybe the server expects "
                    + "TLS first");
        }
    }

    /**
     * Parses the EHLO parameter list of the SMTP AUTH extension keyword.
     *
     * @return the set of SASL mechanism names
     */
    private Set<String> parseMechanismsList(final String authParameters) {
        final String[] mechanisms = authParameters.split(" ");
        return new HashSet<>(Arrays.asList(mechanisms));
    }

    /**
     * Creates the base64 encoded SASL PLAIN initial response.
     */
    private String constructInitialClientResponse() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(512);
        out.write(0);
        out.write(user.getBytes(StandardCharsets.UTF_8));
        out.write(0);
        out.write(password.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(out.toByteArray(), false);
    }
}
