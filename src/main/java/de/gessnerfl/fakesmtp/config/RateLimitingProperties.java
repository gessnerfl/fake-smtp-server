package de.gessnerfl.fakesmtp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Component
@ConfigurationProperties(prefix = "fakesmtp.webapp.rate-limiting")
@Validated
public class RateLimitingProperties {
    
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final int DEFAULT_WINDOW_MINUTES = 1;
    private static final int DEFAULT_CLEANUP_INTERVAL_MINUTES = 1;
    private static final int MAX_ALLOWED_ATTEMPTS = 100;
    private static final int MAX_WINDOW_MINUTES = 60;
    
    private boolean enabled = false;
    
    @Min(value = 1, message = "max-attempts must be at least 1")
    @Max(value = MAX_ALLOWED_ATTEMPTS, message = "max-attempts cannot exceed " + MAX_ALLOWED_ATTEMPTS)
    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    
    @Min(value = 1, message = "window-minutes must be at least 1")
    @Max(value = MAX_WINDOW_MINUTES, message = "window-minutes cannot exceed " + MAX_WINDOW_MINUTES)
    private int windowMinutes = DEFAULT_WINDOW_MINUTES;
    
    @Min(value = 1, message = "cleanup-interval-minutes must be at least 1")
    private int cleanupIntervalMinutes = DEFAULT_CLEANUP_INTERVAL_MINUTES;
    
    private boolean whitelistLocalhost = false;
    private boolean trustProxyHeaders = false;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    
    public int getWindowMinutes() {
        return windowMinutes;
    }
    
    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }
    
    public int getCleanupIntervalMinutes() {
        return cleanupIntervalMinutes;
    }
    
    public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
        this.cleanupIntervalMinutes = cleanupIntervalMinutes;
    }
    
    public boolean isWhitelistLocalhost() {
        return whitelistLocalhost;
    }
    
    public void setWhitelistLocalhost(boolean whitelistLocalhost) {
        this.whitelistLocalhost = whitelistLocalhost;
    }

    public boolean isTrustProxyHeaders() {
        return trustProxyHeaders;
    }

    public void setTrustProxyHeaders(boolean trustProxyHeaders) {
        this.trustProxyHeaders = trustProxyHeaders;
    }
}
