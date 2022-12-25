package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.auth.EasyAuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.server.SMTPServer;

import java.net.InetAddress;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpServerConfiguratorTest {

    @Mock
    private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    @Mock
    private BasicUsernamePasswordValidator basicUsernamePasswordValidator;
    @Mock
    private Logger logger;

    @InjectMocks
    private SmtpServerConfigurator sut;

    @Test
    void shouldConfigureBasicParameters(){
        var port = 1234;
        var bindingAddress = mock(InetAddress.class);
        when(fakeSmtpConfigurationProperties.getPort()).thenReturn(port);
        when(fakeSmtpConfigurationProperties.getBindAddress()).thenReturn(bindingAddress);

        var smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer).setPort(port);
        verify(smtpServer).setBindAddress(bindingAddress);
        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
    }

    @Test
    void shouldConfigureAuthenticationWhenAuthenticationIsConfiguredProperly(){
        var username = "username";
        var password = "password";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn(password);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        var smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        var argumentCaptor = ArgumentCaptor.forClass(AuthenticationHandlerFactory.class);
        verify(smtpServer).setAuthenticationHandlerFactory(argumentCaptor.capture());

        var authenticationHandlerFactory = argumentCaptor.getValue();
        assertNotNull(authenticationHandlerFactory);
        assertThat(authenticationHandlerFactory, instanceOf(EasyAuthenticationHandlerFactory.class));

        var easyAuthenticationHandlerFactory = (EasyAuthenticationHandlerFactory)authenticationHandlerFactory;
        assertSame(basicUsernamePasswordValidator, easyAuthenticationHandlerFactory.getValidator());
    }

    @Test
    void shouldSkipConfigurationOfAuthenticationWhenUsernameIsNull(){
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(null);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        var smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(logger).error(startsWith("Username"));
    }

    @Test
    void shouldSkipConfigurationOfAuthenticationWhenUsernameIsEmptyString(){
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn("");
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        var smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(logger).error(startsWith("Username"));
    }

    @Test
    void shouldSkipConfigurationOfAuthenticationWhenPasswordIsNull(){
        var username = "username";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn(null);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        var smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(logger).error(startsWith("Password"));
    }

    @Test
    void shouldSkipConfigurationOfAuthenticationWhenPasswordIsEmptyString(){
        var username = "username";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn("");
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        var smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(logger).error(startsWith("Password"));
    }

}