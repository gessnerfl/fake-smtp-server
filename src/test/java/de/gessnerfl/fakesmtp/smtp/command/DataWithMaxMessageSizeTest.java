package de.gessnerfl.fakesmtp.smtp.command;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@ActiveProfiles({"integrationtest_with_max_size"})
class DataWithMaxMessageSizeTest extends AbstractCommandIntegrationTest {

    private static final String OVERSIZED_LINE = "a".repeat(8192);
    private static final int OVERSIZED_LINE_COUNT = 129;

    @Test
    void shouldRejectOversizedMessagesDuringDataAndKeepSessionSynchronized() throws Exception {
        this.expect("220");

        this.send("EHLO foo.com");
        this.expectContains("250-SIZE 1048576");

        this.send("MAIL FROM:<validuser@example.com>");
        this.expect("250 Ok");

        this.send("RCPT TO:<success@example.com>");
        this.expect("250 Ok");

        this.send("DATA");
        this.expect("354 End data with <CR><LF>.<CR><LF>");

        for (int i = 0; i < OVERSIZED_LINE_COUNT; i++) {
            this.send(OVERSIZED_LINE);
        }
        this.send(".");

        this.expect("552 5.3.4 Message size exceeds fixed limit");

        this.send("NOOP");
        this.expect("250 Ok");
    }
}
