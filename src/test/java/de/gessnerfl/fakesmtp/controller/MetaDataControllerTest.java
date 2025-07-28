package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.config.FakeSmtpAuthenticationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaDataControllerTest {

    @Mock
    private BuildProperties buildProperties;

    @Mock
    private FakeSmtpAuthenticationProperties authProperties;

    @InjectMocks
    private MetaDataController sut;

    @Test
    void shouldReturnApplicationVersionAndAuthenticationStatus() {
        final var version = "app-version";
        final var authEnabled = true;

        when(buildProperties.getVersion()).thenReturn(version);
        when(authProperties.isAuthenticationEnabled()).thenReturn(authEnabled);

        final var meta = sut.get();

        assertEquals(version, meta.getVersion());
        assertEquals(authEnabled, meta.isAuthenticationEnabled());
        verify(buildProperties).getVersion();
        verify(authProperties).isAuthenticationEnabled();
    }

    @Test
    void shouldReturnApplicationVersionAndAuthenticationDisabled() {
        final var version = "app-version";
        final var authEnabled = false;

        when(buildProperties.getVersion()).thenReturn(version);
        when(authProperties.isAuthenticationEnabled()).thenReturn(authEnabled);

        final var meta = sut.get();

        assertEquals(version, meta.getVersion());
        assertEquals(authEnabled, meta.isAuthenticationEnabled());
        verify(buildProperties).getVersion();
        verify(authProperties).isAuthenticationEnabled();
    }
}
