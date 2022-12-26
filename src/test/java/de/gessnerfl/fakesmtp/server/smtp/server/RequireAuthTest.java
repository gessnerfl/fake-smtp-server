package de.gessnerfl.fakesmtp.server.smtp.server;

import de.gessnerfl.fakesmtp.server.smtp.auth.EasyAuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.auth.LoginFailedException;
import de.gessnerfl.fakesmtp.server.smtp.auth.UsernamePasswordValidator;
import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.server.smtp.util.ServerTestCase;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;

/**
 * @author Evgeny Naumenko
 */
public class RequireAuthTest extends ServerTestCase {
    static final String REQUIRED_USERNAME = "myUserName";

    static final String REQUIRED_PASSWORD = "mySecret01";

    static class RequiredUsernamePasswordValidator implements UsernamePasswordValidator {
        @Override
        public void login(final String username, final String password) throws LoginFailedException {
            if (!username.equals(REQUIRED_USERNAME) || !password.equals(REQUIRED_PASSWORD)) {
                throw new LoginFailedException();
            }
        }
    }

    @Override
    protected TestWiser createTestWiser() {
        var validator = new RequiredUsernamePasswordValidator();
        var fact = new EasyAuthenticationHandlerFactory(validator);
        var wiser = new TestWiser();
        wiser.setHostname("localhost");
        wiser.setPort(PORT);
        wiser.getServer().setAuthenticationHandlerFactory(fact);
        wiser.getServer().setRequireAuth(true);
        return wiser;
    }

    @Test
    public void testAuthRequired() throws Exception {
        this.expect("220");

        this.send("HELO foo.com");
        this.expect("250");

        this.send("EHLO foo.com");
        this.expect("250");

        this.send("NOOP");
        this.expect("250");

        this.send("RSET");
        this.expect("250");

        this.send("MAIL FROM: test@example.com");
        this.expect("530 5.7.0  Authentication required");

        this.send("RCPT TO: test@example.com");
        this.expect("530 5.7.0  Authentication required");

        this.send("DATA");
        this.expect("530 5.7.0  Authentication required");

        this.send("STARTTLS");
        this.expect("454 TLS not supported");

        this.send("QUIT");
        this.expect("221 Bye");
    }

    @Test
    public void testAuthSuccess() throws Exception {
        this.expect("220");

        this.send("HELO foo.com");
        this.expect("250");

        this.send("AUTH LOGIN");
        this.expect("334");

        final String enc_username = Base64Utils.encodeToString(REQUIRED_USERNAME.getBytes(StandardCharsets.US_ASCII));

        this.send(enc_username);
        this.expect("334");

        final String enc_pwd = Base64Utils.encodeToString(REQUIRED_PASSWORD.getBytes(StandardCharsets.US_ASCII));
        this.send(enc_pwd);
        this.expect("235");

        this.send("MAIL FROM: test@example.com");
        this.expect("250");

        this.send("RCPT TO: test@example.com");
        this.expect("250");

        this.send("DATA");
        this.expect("354");

        this.send("\r\n.");
        this.expect("250");

        this.send("QUIT");
        this.expect("221 Bye");
    }
}
