package de.gessnerfl.fakesmtp.server.impl;

import org.junit.Test;
import org.subethamail.smtp.server.SMTPServer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SmtpServerImplTest {
    
    @Test
    public void shouldCreateNewInstanceAndDelegateCallsToRealImplementation(){
        SMTPServer delegate = mock(SMTPServer.class);

        SmtpServerImpl sut = new SmtpServerImpl(delegate);

        sut.start();
        verify(delegate).start();

        sut.stop();
        verify(delegate).stop();
    }

}