package de.gessnerfl.fakesmtp.smtp.server;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import static org.mockito.Mockito.mock;

@Profile("integrationtest")
@Service
public class MockSmtpServerFactory implements SmtpServerFactory {
    @Override
    public SmtpServer create() {
        return mock(SmtpServer.class);
    }
}
