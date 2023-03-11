package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.smtp.command.AbstractCommandIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@ActiveProfiles("integrationtest_with_tls_required")
class RequireTlsTest extends AbstractCommandIntegrationTest {
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
