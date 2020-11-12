package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.subethamail.smtp.auth.LoginFailedException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

    @Test
    public void shouldThrowLoginFailedExceptionWhenUsernameIsNotValid() {
        Assertions.assertThrows(LoginFailedException.class, () -> {
            var username = "username";
            var invalidUsername = "inValidUsername";
            var password = "password";
            var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
            when(authentication.getUsername()).thenReturn(username);
            when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

            sut.login(invalidUsername, password);
        });
    }

    @Test
    public void shouldThrowLoginFailedExceptionWhenPasswordIsNotValid() {
        Assertions.assertThrows(LoginFailedException.class, () -> {
            var username = "username";
            var password = "password";
            var invalidPassword = "invalidPassword";
            var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
            when(authentication.getUsername()).thenReturn(username);
            when(authentication.getPassword()).thenReturn(password);
            when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

            sut.login(username, invalidPassword);
        });
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenAuthenticationIsMissing() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            var username = "username";
            var password = "password";
            when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(null);

            sut.login(username, password);
        });
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenUsernameIsMissingInAuthentication() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            var username = "username";
            var password = "password";
            var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
            when(authentication.getUsername()).thenReturn(null);
            when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

            sut.login(username, password);
        });
    }

    @Test
    public void shouldThrowNullPointerExceptionWhenPasswordIsMissingInAuthentication() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            var username = "username";
            var password = "password";
            var authentication = mock(FakeSmtpConfigurationProperties.Authentication.class);
            when(authentication.getUsername()).thenReturn(username);
            when(authentication.getPassword()).thenReturn(null);
            when(fakeSmtpConfigurationProperties.getAuthentication()).thenReturn(authentication);

            sut.login(username, password);
        });
    }

}