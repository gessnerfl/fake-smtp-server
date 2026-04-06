package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.smtp.command.AbstractCommandIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles({"integrationtest_with_auth"})
@TestPropertySource(properties = "logging.level.de.gessnerfl.fakesmtp.smtp.server.Session=DEBUG")
class AuthLoggingRedactionIntegrationTest extends AbstractCommandIntegrationTest {

    @Test
    void shouldRedactAuthPlainPayloadInDebugLogs(CapturedOutput output) throws Exception {
        this.expect("220");

        this.send("HELO foo.com");
        this.expect("250");

        this.send("AUTH PLAIN AG15VXNlcm5hbWUAbXlTZWNyZXRQYXNzd29yZA==");
        this.expect("235");

        assertThat(output).contains("Client: AUTH PLAIN <redacted>");
        assertThat(output).doesNotContain("Client: AUTH PLAIN AG15VXNlcm5hbWUAbXlTZWNyZXRQYXNzd29yZA==");
        assertThat(output).doesNotContain("Optional[AUTH PLAIN AG15VXNlcm5hbWUAbXlTZWNyZXRQYXNzd29yZA==]");
    }
}
