package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.server.SmtpServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SmtpServerFactoryImplTest {
    private final int PORT = 25;

    @Mock
    private SmtpServerConfigurator configurator;
    @Mock
    private EmailPersister emailPersister;

    @InjectMocks
    private SmtpServerFactoryImpl sut;

    @Test
    public void shouldCreateAndConfigureNewInsance(){
        SmtpServer smtpServer = sut.create();

        assertThat(smtpServer, instanceOf(SmtpServerImpl.class));
        SmtpServerImpl impl = (SmtpServerImpl)smtpServer;
        assertNotNull(impl.smtpServer);

        verify(configurator).configure(impl.smtpServer);
    }

}