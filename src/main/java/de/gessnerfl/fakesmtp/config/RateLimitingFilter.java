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
        
        if (!rateLimiter.isAllowed(clientIp)) {
            logger.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, request.getRequestURI());
            
            long retryAfterSeconds = rateLimiter.getSecondsUntilReset(clientIp);
            long retryAfter = retryAfterSeconds > 0 ? retryAfterSeconds : properties.getWindowMinutes() * 60L;
            
            response.setStatus(429);
            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfter));
            response.setHeader(RATE_LIMIT_REMAINING_HEADER, "0");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }
        
        int remainingAttempts = rateLimiter.getRemainingAttempts(clientIp);
        if (remainingAttempts < Integer.MAX_VALUE) {
            response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remainingAttempts));
        }
        
        filterChain.doFilter(request, response);
        
        if (response.getStatus() == LOGIN_FAILED_STATUS) {
            InMemoryRateLimiter.FailedAttemptResult failedAttemptResult = rateLimiter.recordFailedAttempt(clientIp);
            if (failedAttemptResult.shouldBlockCurrentRequest() && !response.isCommitted()) {
                long retryAfter = failedAttemptResult.retryAfterSeconds() > 0
                        ? failedAttemptResult.retryAfterSeconds()
                        : properties.getWindowMinutes() * 60L;

                response.resetBuffer();
                response.setStatus(429);
                response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfter));
                response.setHeader(RATE_LIMIT_REMAINING_HEADER, "0");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            }
        }
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
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String[] ips = xForwardedFor.split(",");
                // Take the first IP from the chain if it's not a private IP
                // If all IPs are private, fall back to remoteAddr
                String firstIp = null;
                for (String ip : ips) {
                    String trimmed = ip.trim();
                    if (!trimmed.isEmpty()) {
                        if (firstIp == null) {
                            firstIp = trimmed;
                        }
                        // Accept first non-private IP
                        if (!isPrivateIp(trimmed)) {
                            return trimmed;
                        }
                    }
                }
                // All IPs are private, use first one or remoteAddr
                if (firstIp != null) {
                    return firstIp;
                }
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp.trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    private boolean isPrivateIp(String ip) {
        // Check for private IP ranges to prevent spoofing via X-Forwarded-For
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172.16.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            try {
                int secondOctet = Integer.parseInt(ip.split("\\.")[1]);
                return secondOctet >= 16 && secondOctet <= 31;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }
}
