package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.config.WebappAuthenticationProperties;
import de.gessnerfl.fakesmtp.config.WebappSessionProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaDataControllerTest {

    @Mock
    private BuildProperties buildProperties;

    @Mock
    private WebappAuthenticationProperties authProperties;

    @Mock
    private CsrfTokenRepository csrfTokenRepository;

    @Mock
    private WebappSessionProperties sessionProperties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    void shouldReturnApplicationVersionAndAuthenticationStatus() {
        final var version = "app-version";
        final var authEnabled = true;
        final var timeoutMinutes = 10;

        when(buildProperties.getVersion()).thenReturn(version);
        when(authProperties.isAuthenticationEnabled()).thenReturn(authEnabled);
        when(sessionProperties.getSessionTimeoutMinutes()).thenReturn(timeoutMinutes);

        MetaDataController sut = new MetaDataController(buildProperties, authProperties, csrfTokenRepository, sessionProperties);

        final var authentication = new UsernamePasswordAuthenticationToken("user", "pass", java.util.Collections.emptyList());
        final var csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "XSRF-TOKEN", "token");
        final var meta = sut.get(authentication, csrfToken, request, response);

        assertEquals(version, meta.getVersion());
        assertEquals(authEnabled, meta.isAuthenticationEnabled());
        assertTrue(meta.isAuthenticated());
        assertEquals(timeoutMinutes, meta.getSessionTimeoutMinutes());
        verify(buildProperties).getVersion();
        verify(authProperties).isAuthenticationEnabled();
    }

    @Test
    void shouldReturnApplicationVersionAndAuthenticationDisabled() {
        final var version = "app-version";
        final var authEnabled = false;
        final var timeoutMinutes = 15;

        when(buildProperties.getVersion()).thenReturn(version);
        when(authProperties.isAuthenticationEnabled()).thenReturn(authEnabled);
        when(sessionProperties.getSessionTimeoutMinutes()).thenReturn(timeoutMinutes);

        MetaDataController sut = new MetaDataController(buildProperties, authProperties, csrfTokenRepository, sessionProperties);

        final var meta = sut.get(null, null, request, response);

        assertEquals(version, meta.getVersion());
        assertEquals(authEnabled, meta.isAuthenticationEnabled());
        assertFalse(meta.isAuthenticated());
        assertEquals(timeoutMinutes, meta.getSessionTimeoutMinutes());
        verify(buildProperties).getVersion();
        verify(authProperties).isAuthenticationEnabled();
    }
}
