package de.gessnerfl.fakesmtp.config;

import de.gessnerfl.fakesmtp.smtp.auth.BasicUsernamePasswordValidator;
import de.gessnerfl.fakesmtp.smtp.command.CommandHandler;
import de.gessnerfl.fakesmtp.smtp.server.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import de.gessnerfl.fakesmtp.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.smtp.auth.EasyAuthenticationHandlerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.ResourceLoader;

import java.net.InetAddress;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseSmtpServerConfigTest {

    @Mock
    private BuildProperties buildProperties;
    @Mock
    private BaseMessageListener baseMessageListener;
    @Mock
    private CommandHandler commandHandler;
    @Mock
    private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    @Mock
    private BasicUsernamePasswordValidator basicUsernamePasswordValidator;
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private BaseSmtpServer smtpServer;
    @Mock
    private Logger logger;

    private BaseSmtpServerConfig sut;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        sut = spy(new BaseSmtpServerConfig(buildProperties, resourceLoader, fakeSmtpConfigurationProperties, Collections.singletonList(baseMessageListener), basicUsernamePasswordValidator, commandHandler, true, logger));
        when(sut.createBaseSmtpServerFor(any(MessageListenerAdapter.class), any(SessionIdFactory.class))).thenReturn(smtpServer);
    }

    @Test
    void shouldConfigureBasicParameters() {
        var port = 1234;
        var bindingAddress = mock(InetAddress.class);
        when(fakeSmtpConfigurationProperties.getPort()).thenReturn(port);
        when(fakeSmtpConfigurationProperties.getBindAddress()).thenReturn(bindingAddress);

        SmtpServer result = sut.smtpServer();

        assertSame(smtpServer, result);
        verify(smtpServer).setPort(port);
        verify(smtpServer).setBindAddress(bindingAddress);
        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(smtpServer, never()).setRequireAuth(true);
    }

    @Test
    void shouldConfigureAuthenticationWhenAuthenticationIsConfiguredProperly() {
        var username = "username";
        var password = "password";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn(password);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        SmtpServer result = sut.smtpServer();

        assertSame(smtpServer, result);

        var argumentCaptor = ArgumentCaptor.forClass(AuthenticationHandlerFactory.class);
        verify(smtpServer).setAuthenticationHandlerFactory(argumentCaptor.capture());
        verify(smtpServer).setRequireAuth(true);

        var authenticationHandlerFactory = argumentCaptor.getValue();
        assertNotNull(authenticationHandlerFactory);
        assertThat(authenticationHandlerFactory, instanceOf(EasyAuthenticationHandlerFactory.class));

        var easyAuthenticationHandlerFactory = (EasyAuthenticationHandlerFactory) authenticationHandlerFactory;
        assertSame(basicUsernamePasswordValidator, easyAuthenticationHandlerFactory.getValidator());
    }

    @Test
    void shouldSkipConfigurationOfAuthenticationWhenUsernameIsNull() {
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(null);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        SmtpServer result = sut.smtpServer();

        assertSame(smtpServer, result);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(smtpServer, never()).setRequireAuth(true);
        verify(logger).error(startsWith("Username"));
    }

    @Test
    void shouldSkipConfigurationOfAuthenticationWhenUsernameIsEmptyString() {
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn("");
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        SmtpServer result = sut.smtpServer();

        assertSame(smtpServer, result);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(smtpServer, never()).setRequireAuth(true);
        verify(logger).error(startsWith("Username"));
    }

    @Test
    void shouldSkipConfigurationOfAuthenticationWhenPasswordIsNull() {
        var username = "username";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn(null);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        SmtpServer result = sut.smtpServer();

        assertSame(smtpServer, result);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(smtpServer, never()).setRequireAuth(true);
        verify(logger).error(startsWith("Password"));
    }

    @Test
    void shouldSkipConfigurationOfAuthenticationWhenPasswordIsEmptyString() {
        var username = "username";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn("");
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        SmtpServer result = sut.smtpServer();

        assertSame(smtpServer, result);

        verify(smtpServer, never()).setAuthenticationHandlerFactory(any(AuthenticationHandlerFactory.class));
        verify(smtpServer, never()).setRequireAuth(true);
        verify(logger).error(startsWith("Password"));
    }

}