package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.auth.EasyAuthenticationHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;

import java.net.InetAddress;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SmtpServerConfiguratorTest {

    @Mock
    private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    @Mock
    private BasicUsernamePasswordValidator basicUsernamePasswordValidator;
    @Mock
    private Logger logger;

    @InjectMocks
    private SmtpServerConfigurator sut;

    @Test
    public void shouldConfigureBasicParameters(){
        final Integer port = 1234;
        final InetAddress bindingAddress = mock(InetAddress.class);
        when(fakeSmtpConfigurationProperties.getPort()).thenReturn(port);
        when(fakeSmtpConfigurationProperties.getBindAddress()).thenReturn(bindingAddress);

        final SMTPServer smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer).setPort(port);
        verify(smtpServer).setBindAddress(bindingAddress);
        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
    }

    @Test
    public void shouldConfigureAuthenticationWhenAuthenticationIsConfiguredProperly(){
        final String username = "username";
        final String password = "password";
        final FakeSmtpConfigurationProperties.Authentication authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn(password);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        final SMTPServer smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        ArgumentCaptor<AuthenticationHandlerFactory> argumentCaptor = ArgumentCaptor.forClass(AuthenticationHandlerFactory.class);
        verify(smtpServer).setAuthenticationHandlerFactory(argumentCaptor.capture());

        AuthenticationHandlerFactory authenticationHandlerFactory = argumentCaptor.getValue();
        assertNotNull(authenticationHandlerFactory);
        assertThat(authenticationHandlerFactory, instanceOf(EasyAuthenticationHandlerFactory.class));

        EasyAuthenticationHandlerFactory easyAuthenticationHandlerFactory = (EasyAuthenticationHandlerFactory)authenticationHandlerFactory;
        assertSame(basicUsernamePasswordValidator, easyAuthenticationHandlerFactory.getValidator());
    }

    @Test
    public void shouldSkipConfigurationOfAuthenticationWhenUsernameIsNull(){
        final String password = "password";
        final FakeSmtpConfigurationProperties.Authentication authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(null);
        when(authentication.getPassword()).thenReturn(password);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        final SMTPServer smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(logger).error(startsWith("Username"));
    }

    @Test
    public void shouldSkipConfigurationOfAuthenticationWhenUsernameIsEmptyString(){
        final String password = "password";
        final FakeSmtpConfigurationProperties.Authentication authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn("");
        when(authentication.getPassword()).thenReturn(password);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        final SMTPServer smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(logger).error(startsWith("Username"));
    }

    @Test
    public void shouldSkipConfigurationOfAuthenticationWhenPasswordIsNull(){
        final String username = "username";
        final FakeSmtpConfigurationProperties.Authentication authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn(null);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        final SMTPServer smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(logger).error(startsWith("Password"));
    }

    @Test
    public void shouldSkipConfigurationOfAuthenticationWhenPasswordIsEmptyString(){
        final String username = "username";
        final FakeSmtpConfigurationProperties.Authentication authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn("");
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        final SMTPServer smtpServer = mock(SMTPServer.class);

        sut.configure(smtpServer);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(logger).error(startsWith("Password"));
    }

}