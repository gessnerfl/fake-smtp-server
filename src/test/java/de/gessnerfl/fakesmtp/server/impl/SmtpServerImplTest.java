package de.gessnerfl.fakesmtp.server.impl;

import org.junit.jupiter.api.Test;
import de.gessnerfl.fakesmtp.server.smtp.server.SMTPServer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpServerImplTest {
    
    @Test
    void shouldCreateNewInstanceAndDelegateCallsToRealImplementation(){
        var delegate = mock(SMTPServer.class);

        var sut = new SmtpServerImpl(delegate);

        sut.start();
        verify(delegate).start();

        sut.stop();
        verify(delegate).stop();
    }

}