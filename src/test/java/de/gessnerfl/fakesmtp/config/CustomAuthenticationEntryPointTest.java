package de.gessnerfl.fakesmtp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException authException;

    @Mock
    private ViewResolver viewResolver;

    @Mock
    private View view;

    private CustomAuthenticationEntryPoint sut;

    @BeforeEach
    void setUp() {
        sut = new CustomAuthenticationEntryPoint(List.of(viewResolver));
    }

    @Test
    void shouldSendUnauthorizedErrorWhenRequestDoesNotAcceptHtml() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Accept")).thenReturn(MediaType.APPLICATION_JSON_VALUE);

        sut.commence(request, response, authException);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        verifyNoInteractions(viewResolver);
    }

    @Test
    void shouldRenderSpaWhenHtmlRequestTargetsRoot() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Accept")).thenReturn(MediaType.TEXT_HTML_VALUE);
        when(request.getRequestURI()).thenReturn("/");
        when(viewResolver.resolveViewName(eq("index"), any())).thenReturn(view);

        sut.commence(request, response, authException);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType(MediaType.TEXT_HTML_VALUE);
        verify(view).render(eq(Collections.emptyMap()), eq(request), eq(response));
        verify(response, never()).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }

    @Test
    void shouldSendUnauthorizedWhenViewCannotBeResolved() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Accept")).thenReturn(MediaType.TEXT_HTML_VALUE);
        when(request.getRequestURI()).thenReturn("/");
        when(viewResolver.resolveViewName(eq("index"), any())).thenReturn(null);

        sut.commence(request, response, authException);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
