package de.gessnerfl.fakesmtp.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServerTest {

    @Mock
    private SmtpServerFactory smtpServerFactory;
    @Mock
    private Logger logger;

    @InjectMocks
    private EmailServer sut;

    @Test
    public void shouldSetSmtpServerOnPostConstruct(){
        var smtpServer = mock(SmtpServer.class);
        when(smtpServerFactory.create()).thenReturn(smtpServer);

        sut.startServer();

        assertSame(smtpServer, sut.smtpServer);
        verify(smtpServerFactory).create();
        verify(smtpServer).start();
    }

    @Test
    public void shouldStopServerOnPreDestroy(){
        var smtpServer = mock(SmtpServer.class);
        sut.smtpServer = smtpServer;

        sut.shutdown();

        verify(smtpServer).stop();
        verify(logger, times(2)).info(anyString());
    }

    @Test
    public void shouldSilentlyShutdownWhenNoServerIsSet(){
        sut.shutdown();

        verify(logger).debug(anyString());
    }
}