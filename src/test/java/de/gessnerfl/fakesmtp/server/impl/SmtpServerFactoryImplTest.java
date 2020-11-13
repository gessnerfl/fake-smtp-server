package de.gessnerfl.fakesmtp.server.impl;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpServerFactoryImplTest {
    private final int PORT = 25;

    @Mock
    private SmtpServerConfigurator configurator;
    @Mock
    private MessageListener messageListener;

    @InjectMocks
    private SmtpServerFactoryImpl sut;

    @Test
    void shouldCreateAndConfigureNewInsance(){
        var smtpServer = sut.create();

        MatcherAssert.assertThat(smtpServer, instanceOf(SmtpServerImpl.class));
        var impl = (SmtpServerImpl)smtpServer;
        Assertions.assertNotNull(impl.smtpServer);

        verify(configurator).configure(impl.smtpServer);
    }

}