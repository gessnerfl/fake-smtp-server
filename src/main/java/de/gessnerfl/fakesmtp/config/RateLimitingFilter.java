package de.gessnerfl.fakesmtp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final int LOGIN_FAILED_STATUS = HttpServletResponse.SC_UNAUTHORIZED;
    
    private final InMemoryRateLimiter rateLimiter;
    private final RateLimitingProperties properties;
    
    public RateLimitingFilter(InMemoryRateLimiter rateLimiter, RateLimitingProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientIp = extractClientIp(request);

        InMemoryRateLimiter.AttemptReservation reservation = rateLimiter.reserveLoginAttempt(clientIp);
        if (reservation.blocked()) {
            logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, request.getRequestURI());
            writeTooManyRequests(response, reservation.retryAfterSeconds());
            return;
        }

        if (reservation.remainingAttempts() < Integer.MAX_VALUE) {
            response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(reservation.remainingAttempts()));
        }

        boolean failedLogin = false;
        try {
            filterChain.doFilter(request, response);
            failedLogin = response.getStatus() == LOGIN_FAILED_STATUS;
            if (failedLogin) {
                rateLimiter.commitFailedAttempt(reservation);
            }
        } finally {
            if (!failedLogin) {
                rateLimiter.releaseAttempt(reservation);
            }
        }
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        long retryAfter = retryAfterSeconds > 0 ? retryAfterSeconds : properties.getWindowMinutes() * 60L;

        response.setStatus(429);
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfter));
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, "0");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
    }
    
    private boolean isLoginRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        return LOGIN_PATH.equals(path) && "POST".equalsIgnoreCase(request.getMethod());
    }
    
    private String extractClientIp(HttpServletRequest request) {
        if (properties.isTrustProxyHeaders()) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // With trusted proxy headers enabled, the leftmost forwarded value
                // is the client identity asserted by the reverse proxy.
                for (String ip : xForwardedFor.split(",")) {
                    String trimmed = ip.trim();
                    if (!trimmed.isEmpty()) {
                        return trimmed;
                    }
                }
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp.trim();
            }
        }
        
        return request.getRemoteAddr();
    }
}
