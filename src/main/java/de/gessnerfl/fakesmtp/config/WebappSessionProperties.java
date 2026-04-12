package de.gessnerfl.fakesmtp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fakesmtp.webapp.session")
public class WebappSessionProperties {
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 10;
    private static final int MAX_SESSION_TIMEOUT_MINUTES = 1440;
    private static final int DEFAULT_SSE_HEARTBEAT_INTERVAL_SECONDS = 30;
    private static final int DEFAULT_SSE_EVENT_SEND_TIMEOUT_SECONDS = 5;

    private int sessionTimeoutMinutes = DEFAULT_SESSION_TIMEOUT_MINUTES;
    private int sseHeartbeatIntervalSeconds = DEFAULT_SSE_HEARTBEAT_INTERVAL_SECONDS;
    private int sseEventSendTimeoutSeconds = DEFAULT_SSE_EVENT_SEND_TIMEOUT_SECONDS;

    public int getSessionTimeoutMinutes() {
        if (sessionTimeoutMinutes <= 0) {
            return DEFAULT_SESSION_TIMEOUT_MINUTES;
        }
        return Math.min(sessionTimeoutMinutes, MAX_SESSION_TIMEOUT_MINUTES);
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public int getSseHeartbeatIntervalSeconds() {
        return sseHeartbeatIntervalSeconds > 0 ? sseHeartbeatIntervalSeconds : DEFAULT_SSE_HEARTBEAT_INTERVAL_SECONDS;
    }

    public void setSseHeartbeatIntervalSeconds(int sseHeartbeatIntervalSeconds) {
        this.sseHeartbeatIntervalSeconds = sseHeartbeatIntervalSeconds;
    }

    public int getSseEventSendTimeoutSeconds() {
        return sseEventSendTimeoutSeconds > 0 ? sseEventSendTimeoutSeconds : DEFAULT_SSE_EVENT_SEND_TIMEOUT_SECONDS;
    }

    public void setSseEventSendTimeoutSeconds(int sseEventSendTimeoutSeconds) {
        this.sseEventSendTimeoutSeconds = sseEventSendTimeoutSeconds;
    }
}
