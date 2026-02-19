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
            
            response.setStatus(429);
            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(properties.getWindowMinutes() * 60));
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
        
        if (request.getMethod().equalsIgnoreCase("POST")) {
            rateLimiter.recordAttempt(clientIp);
        }
    }
    
    private boolean isLoginRequest(HttpServletRequest request) {
        return LOGIN_PATH.equals(request.getRequestURI()) && 
               "POST".equalsIgnoreCase(request.getMethod());
    }
    
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            for (String ip : ips) {
                String trimmed = ip.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        
        return request.getRemoteAddr();
    }
}
