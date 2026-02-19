package de.gessnerfl.fakesmtp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that catches AuthorizationDeniedException to prevent verbose stack traces
 * from being logged by the servlet container. Converts them to clean 403 responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthorizationExceptionFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationExceptionFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } catch (AuthorizationDeniedException ex) {
            handleAuthorizationDenied(request, response, ex);
        } catch (ServletException ex) {
            // Check if the cause is an AuthorizationDeniedException
            if (ex.getCause() instanceof AuthorizationDeniedException) {
                handleAuthorizationDenied(request, response, (AuthorizationDeniedException) ex.getCause());
            } else {
                throw ex;
            }
        }
    }

    private void handleAuthorizationDenied(HttpServletRequest request, HttpServletResponse response,
                                           AuthorizationDeniedException ex) throws IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = getClientIpAddress(request);

        // Log only as debug - no stack trace needed for expected auth failures
        LOGGER.debug("Access denied for {} {} from {} - {}", method, path, remoteAddr, ex.getMessage());

        // Only send error if response hasn't been committed yet
        if (!response.isCommitted()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
