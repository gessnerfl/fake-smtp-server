package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;

class HelloTest extends AbstractCommandIntegrationTest {

    @Test
    void testHelloCommand() throws Exception {
        this.expect("220");

        this.send("HELO");
        this.expect("501 Syntax: HELO <hostname>");

        this.send("HELO");
        this.expect("501 Syntax: HELO <hostname>");

        // Correct!
        this.send("HELO foo.com");
        this.expect("250");

        // Correct!
        this.send("HELO foo.com");
        this.expect("250");
    }

    @Test
    void testHelloReset() throws Exception {
        this.expect("220");

        this.send("HELO foo.com");
        this.expect("250");

        this.send("MAIL FROM: test@foo.com");
        this.expect("250 Ok");

        this.send("RSET");
        this.expect("250 Ok");

        this.send("MAIL FROM: test@foo.com");
        this.expect("250 Ok");
    }
}
