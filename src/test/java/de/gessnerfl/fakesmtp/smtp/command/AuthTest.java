package de.gessnerfl.fakesmtp.smtp.command;

import de.gessnerfl.fakesmtp.smtp.server.Session;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;

@ActiveProfiles({"integrationtest_with_auth"})
class AuthTest extends AbstractCommandIntegrationTest {
    static final String REQUIRED_USERNAME = "myUsername";

    static final String REQUIRED_PASSWORD = "mySecretPassword";

    /**
     * Test method for AUTH PLAIN. The sequence under test is as follows:
     * <ol>
     * <li>HELO test</li>
     * <li>User starts AUTH PLAIN</li>
     * <li>User sends username+password</li>
     * <li>We expect login to be successful. Also the Base64 transformations are
     * tested.</li>
     * <li>User issues another AUTH command</li>
     * <li>We expect an error message</li>
     * </ol>
     * {@link AuthCommand#execute(java.lang.String, Session)}.
     *
     * @throws Exception on error
     */
    @Test
    void testAuthPlain() throws Exception {
        this.expect("220");

        this.send("HELO foo.com");
        this.expect("250");

        this.send("AUTH PLAIN");
        this.expect("334");

        final String authString
                = new String(new byte[]{0}) + REQUIRED_USERNAME + new String(new byte[]{0}) + REQUIRED_PASSWORD;

        final String enc_authString = Base64Utils.encodeToString(authString.getBytes(StandardCharsets.US_ASCII));
        this.send(enc_authString);
        this.expect("235");

        this.send("AUTH");
        this.expect("503");
    }

    /**
     * Test method for AUTH LOGIN. The sequence under test is as follows:
     * <ol>
     * <li>HELO test</li>
     * <li>User starts AUTH LOGIN</li>
     * <li>User sends username</li>
     * <li>User cancels authentication by sending "*"</li>
     * <li>User restarts AUTH LOGIN</li>
     * <li>User sends username</li>
     * <li>User sends password</li>
     * <li>We expect login to be successful. Also the Base64 transformations are
     * tested.</li>
     * <li>User issues another AUTH command</li>
     * <li>We expect an error message</li>
     * </ol>
     * {@link AuthCommand#execute(java.lang.String, Session)}.
     *
     * @throws Exception on error
     */
    @Test
    void testAuthLogin() throws Exception {
        this.expect("220");

        this.send("HELO foo.com");
        this.expect("250");

        this.send("AUTH LOGIN");
        this.expect("334");

        final String enc_username = Base64Utils.encodeToString(REQUIRED_USERNAME.getBytes(StandardCharsets.US_ASCII));

        this.send(enc_username);
        this.expect("334");

        this.send("*");
        this.expect("501");

        this.send("AUTH LOGIN");
        this.expect("334");

        this.send(enc_username);
        this.expect("334");

        final String enc_pwd = Base64Utils.encodeToString(REQUIRED_PASSWORD.getBytes(StandardCharsets.US_ASCII));
        this.send(enc_pwd);
        this.expect("235");

        this.send("AUTH");
        this.expect("503");
    }
}
