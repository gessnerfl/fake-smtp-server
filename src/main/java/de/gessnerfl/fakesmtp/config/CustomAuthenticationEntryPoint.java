package de.gessnerfl.fakesmtp.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Custom authentication entry point that prevents browsers from showing the native basic auth dialog
 * and renders the SPA entry page for unauthenticated HTML requests so the login UI can load.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);
    private static final String SPA_VIEW_NAME = "index";

    private final List<ViewResolver> viewResolvers;

    public CustomAuthenticationEntryPoint(List<ViewResolver> viewResolvers) {
        this.viewResolvers = Objects.requireNonNull(viewResolvers);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        if (shouldRenderSpaLogin(request)) {
            if (renderSpa(request, response)) {
                return;
            }
            LOGGER.warn("Failed to render SPA login page for unauthenticated request to {}", request.getRequestURI());
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }

    private boolean shouldRenderSpaLogin(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String acceptHeader = request.getHeader("Accept");
        boolean acceptsHtml = acceptHeader == null || acceptHeader.contains(MediaType.TEXT_HTML_VALUE);
        if (!acceptsHtml) {
            return false;
        }

        String requestUri = request.getRequestURI();
        return "/".equals(requestUri) || requestUri.startsWith("/emails");
    }

    private boolean renderSpa(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Locale locale = RequestContextUtils.getLocale(request);

        for (ViewResolver viewResolver : viewResolvers) {
            try {
                View view = viewResolver.resolveViewName(SPA_VIEW_NAME, locale);
                if (view != null) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(MediaType.TEXT_HTML_VALUE);
                    view.render(Collections.emptyMap(), request, response);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.debug("Unable to resolve or render view '{}' with resolver {}", SPA_VIEW_NAME, viewResolver, e);
            }
        }

        return false;
    }
}
