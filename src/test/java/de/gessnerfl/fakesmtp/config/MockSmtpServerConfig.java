package de.gessnerfl.fakesmtp.config;

import de.gessnerfl.fakesmtp.smtp.server.SmtpServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("mockserver")
public class MockSmtpServerConfig implements SmtpServerConfig {
    @Override
    public SmtpServer smtpServer() {
        return mock(SmtpServer.class);
    }
}
