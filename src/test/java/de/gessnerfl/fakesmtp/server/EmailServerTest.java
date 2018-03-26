package de.gessnerfl.fakesmtp.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailServerTest {

    @Mock
    private SmtpServerFactory smtpServerFactory;

    @InjectMocks
    private EmailServer sut;

    @Test
    public void shouldSetSmtpServerOnPostConstruct(){
        SmtpServer smtpServer = mock(SmtpServer.class);
        when(smtpServerFactory.create()).thenReturn(smtpServer);

        sut.startServer();

        assertSame(smtpServer, sut.smtpServer);
        verify(smtpServerFactory).create();
        verify(smtpServer).start();
    }

    @Test
    public void shouldStopServerOnPreDestroy(){
        SmtpServer smtpServer = mock(SmtpServer.class);
        sut.smtpServer = smtpServer;

        sut.shutdown();

        verify(smtpServer).stop();
    }

    @Test
    public void shouldSilentlyShutdownWhenNoServerIsSet(){
        sut.shutdown();

        //No exception expected
    }
}