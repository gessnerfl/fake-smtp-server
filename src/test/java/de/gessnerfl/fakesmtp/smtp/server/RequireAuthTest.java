package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.smtp.StoringMessageListener;
import de.gessnerfl.fakesmtp.smtp.client.Client;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Transactional
@ActiveProfiles({"auth_integrationtest", "default"})
@ExtendWith(SpringExtension.class)
@SpringBootTest
class RequireAuthTest {
    static final String REQUIRED_USERNAME = "myUsername";

    static final String REQUIRED_PASSWORD = "myPassword";
    
    @Autowired
    private SmtpServer smtpServer;
    @Autowired
    private StoringMessageListener storingMessageListener;
    private Client c;
    
    @BeforeEach
    void init() throws IOException {
        c = new Client("localhost", smtpServer.getPort());
        storingMessageListener.reset();
    }
    
    @AfterEach
    void cleanup(){
        storingMessageListener.reset();
    }

    @Test
    void testAuthRequired() throws Exception {
        c.expect("220");

        c.send("HELO foo.com");
        c.expect("250");

        c.send("EHLO foo.com");
        c.expect("250");

        c.send("NOOP");
        c.expect("250");

        c.send("RSET");
        c.expect("250");

        c.send("MAIL FROM: test@example.com");
        c.expect("530 5.7.0  Authentication required");

        c.send("RCPT TO: test@example.com");
        c.expect("530 5.7.0  Authentication required");

        c.send("DATA");
        c.expect("530 5.7.0  Authentication required");

        c.send("STARTTLS");
        c.expect("454 TLS not supported");

        c.send("QUIT");
        c.expect("221 Bye");
    }

    @Test
    void testAuthSuccess() throws Exception {
        c.expect("220");

        c.send("HELO foo.com");
        c.expect("250");

        c.send("AUTH LOGIN");
        c.expect("334");

        final String enc_username = Base64.getEncoder().encodeToString(REQUIRED_USERNAME.getBytes(StandardCharsets.US_ASCII));

        c.send(enc_username);
        c.expect("334");

        final String enc_pwd = Base64.getEncoder().encodeToString(REQUIRED_PASSWORD.getBytes(StandardCharsets.US_ASCII));
        c.send(enc_pwd);
        c.expect("235");

        c.send("MAIL FROM: test@example.com");
        c.expect("250");

        c.send("RCPT TO: test@example.com");
        c.expect("250");

        c.send("DATA");
        c.expect("354");

        c.send("\r\n.");
        c.expect("250");

        c.send("QUIT");
        c.expect("221 Bye");
    }
}
