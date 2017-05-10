package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import de.gessnerfl.fakesmtp.server.SmtpServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.InetAddress;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SmtpServerFactoryImplTest {
    private final int PORT = 25;

    @Mock
    private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    @Mock
    private EmailPersister emailPersister;

    @InjectMocks
    private SmtpServerFactoryImpl sut;

    @Test
    public void shouldCreateNewInstanceWhenBindingAddressIsSet(){
        when(fakeSmtpConfigurationProperties.getPort()).thenReturn(PORT);
        when(fakeSmtpConfigurationProperties.getBindAddress()).thenReturn(null);

        SmtpServer smtpServer = sut.create();

        assertThat(smtpServer, instanceOf(SmtpServerImpl.class));
        SmtpServerImpl impl = (SmtpServerImpl)smtpServer;
        assertNotNull(impl.smtpServer);
        assertEquals(PORT, impl.smtpServer.getPort());
        assertNull(impl.smtpServer.getBindAddress());
    }

    @Test
    public void shouldCreateNewInstanceWhenNoBindingAddressIsSet() throws Exception {
        InetAddress bindingAddress = InetAddress.getByName("127.0.0.1");
        when(fakeSmtpConfigurationProperties.getPort()).thenReturn(PORT);
        when(fakeSmtpConfigurationProperties.getBindAddress()).thenReturn(bindingAddress);

        SmtpServer smtpServer = sut.create();

        assertThat(smtpServer, instanceOf(SmtpServerImpl.class));
        SmtpServerImpl impl = (SmtpServerImpl)smtpServer;
        assertNotNull(impl.smtpServer);
        assertEquals(PORT, impl.smtpServer.getPort());
        assertEquals(bindingAddress, impl.smtpServer.getBindAddress());
    }

}