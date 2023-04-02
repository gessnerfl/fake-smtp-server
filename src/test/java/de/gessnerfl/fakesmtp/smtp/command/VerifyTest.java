package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;

class VerifyTest extends AbstractCommandIntegrationTest {

    @Test
    void testHelloCommand() throws Exception {
        this.expect("220");

        this.send("VRFY");
        this.expect("502 VRFY command is disabled");
    }
}
