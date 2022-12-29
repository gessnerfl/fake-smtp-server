package de.gessnerfl.fakesmtp.smtp.server;

import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.smtp.util.ServerTestCase;

class RequireTlsTest extends ServerTestCase {

    @Override
    protected TestWiser createTestWiser(int serverPort) {
        var wiser = new TestWiser();
        wiser.setHostname("localhost");
        wiser.setPort(serverPort);
        wiser.getServer().setRequireTLS(true);
        return wiser;
    }

    @Test
    void testNeedSTARTTLS() throws Exception {
        this.expect("220");

        this.send("HELO foo.com");
        this.expect("530 Must issue a STARTTLS command first");

        this.send("EHLO foo.com");
        this.expect("250");

        this.send("NOOP");
        this.expect("250");

        this.send("MAIL FROM: test@example.com");
        this.expect("530 Must issue a STARTTLS command first");

        this.send("STARTTLS foo");
        this.expect("501 Syntax error (no parameters allowed)");

        this.send("QUIT");
        this.expect("221 Bye");
    }
}
