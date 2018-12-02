package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.subethamail.smtp.auth.LoginFailedException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BasicUsernamePasswordValidatorTest {

    @Mock
    private FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;

    @InjectMocks
    private BasicUsernamePasswordValidator sut;


    @Test
    public void shouldSuccessfullyValidateCorrectUsernameAndPassword() throws Exception {
        var username = "username";
        var password = "password";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn(password);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        sut.login(username, password);

        verify(authentication).getUsername();
        verify(authentication).getPassword();
    }

    @Test(expected = LoginFailedException.class)
    public void shouldThrowLoginFailedExceptionWhenUsernameIsNotValid() throws Exception {
        var username = "username";
        var invalidUsername = "inValidUsername";
        var password = "password";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        sut.login(invalidUsername, password);
    }

    @Test(expected = LoginFailedException.class)
    public void shouldThrowLoginFailedExceptionWhenPasswordIsNotValid() throws Exception {
        var username = "username";
        var password = "password";
        var invalidPassword = "invalidPassword";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn(password);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        sut.login(username, invalidPassword);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenAuthenticationIsMissing() throws Exception {
        var username = "username";
        var password = "password";
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(null);

        sut.login(username, password);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenUsernameIsMissingInAuthentication() throws Exception {
        var username = "username";
        var password = "password";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(null);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        sut.login(username, password);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenPasswordIsMissingInAuthentication() throws Exception {
        var username = "username";
        var password = "password";
        var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
        when(authentication.getUsername()).thenReturn(username);
        when(authentication.getPassword()).thenReturn(null);
        when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

        sut.login(username, password);
    }

}