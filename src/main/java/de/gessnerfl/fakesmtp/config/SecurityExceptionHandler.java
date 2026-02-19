package de.gessnerfl.fakesmtp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for security-related exceptions.
 * Suppresses verbose stack traces for expected authorization errors.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationDenied(
            AuthorizationDeniedException ex,
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();

        // Log only as debug - no stack trace needed for expected auth failures
        LOGGER.debug("Access denied for {} {} from {} - {}", method, path, remoteAddr, ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "Access denied");
        body.put("path", path);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
