package de.gessnerfl.fakesmtp.smtp.command;

import de.gessnerfl.fakesmtp.smtp.server.BaseSmtpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HelpTest extends AbstractCommandIntegrationTest {

    @Autowired
    private BaseSmtpServer baseSmtpServer;

    @Test
    void testServerHelloCommand() throws Exception {
        this.expect("220");

        this.send("HELP");
        this.expect("214-" + baseSmtpServer.getSoftwareName() + " on " + baseSmtpServer.getHostName() + "\n" +
                "214-Topics:\n" +
                "214-     AUTH\n" +
                "214-     DATA\n" +
                "214-     EHLO\n" +
                "214-     HELO\n" +
                "214-     HELP\n" +
                "214-     MAIL\n" +
                "214-     NOOP\n" +
                "214-     QUIT\n" +
                "214-     RCPT\n" +
                "214-     RSET\n" +
                "214-     STARTTLS\n" +
                "214-     VRFY\n" +
                "214-     EXPN\n" +
                "214-For more info use \"HELP <topic>\".\n" +
                "214 End of HELP info");
    }

    @Test
    void testCommandHelloCommand() throws Exception {
        this.expect("220");

        this.send("HELP NOOP");
        this.expect("214-NOOP\n" +
                "214-    The noop command\n" +
                "214 End of NOOP info");
    }

    @Test
    void testInvalidCommandHelloCommand() throws Exception {
        this.expect("220");

        this.send("HELP FOO");
        this.expect("504 HELP topic \"FOO\" unknown.");
    }
}
