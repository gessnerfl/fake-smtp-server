package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;

class ExpandTest extends AbstractCommandIntegrationTest {

    @Test
    void testHelloCommand() throws Exception {
        this.expect("220");

        this.send("EXPN");
        this.expect("502 EXPN command is disabled");
    }
}
