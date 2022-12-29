package de.gessnerfl.fakesmtp.smtp.server;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BaseSmtpServerFactoryTest {
    private final int PORT = 25;

    @Mock
    private BaseSmtpServerConfigurator configurator;
    @Mock
    private BaseMessageListener baseMessageListener;

    @InjectMocks
    private BaseSmtpServerFactory sut;

    @Test
    void shouldCreateAndConfigureNewInsance(){
        var smtpServer = sut.create();

        MatcherAssert.assertThat(smtpServer, instanceOf(BaseSmtpServer.class));
        verify(configurator).configure((BaseSmtpServer) smtpServer);
    }

}