package de.gessnerfl.fakesmtp.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "fakesmtp.webapp.authentication")
public class WebappAuthenticationProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebappAuthenticationProperties.class);

    private Boolean enabled;
    private String username;
    private String password;
    private int concurrentSessions = 1;

    @PostConstruct
    void initialize() {
        validateConfiguration();
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getConcurrentSessions() {
        return concurrentSessions;
    }

    public void setConcurrentSessions(int concurrentSessions) {
        this.concurrentSessions = concurrentSessions;
    }

    public boolean isAuthenticationEnabled() {
        if (enabled != null) {
            return enabled;
        }
        return hasCredentialsConfigured();
    }

    void validateConfiguration() {
        boolean hasUsername = StringUtils.hasText(username);
        boolean hasPassword = StringUtils.hasText(password);

        if (enabled == null) {
            if (hasUsername != hasPassword) {
                throw new IllegalStateException("Both username and password must be configured together for Web UI authentication");
            }
            if (hasUsername) {
                LOGGER.warn("Implicit Web UI authentication enablement via username/password is deprecated; set fakesmtp.webapp.authentication.enabled explicitly.");
            }
            return;
        }

        if (enabled && (!hasUsername || !hasPassword)) {
            throw new IllegalStateException("Web UI authentication requires non-empty username and password when enabled=true");
        }

        if (!enabled && (hasUsername || hasPassword)) {
            throw new IllegalStateException("Web UI authentication must not configure username or password when enabled=false");
        }
    }

    boolean hasCredentialsConfigured() {
        return StringUtils.hasText(username) && StringUtils.hasText(password);
    }
}
