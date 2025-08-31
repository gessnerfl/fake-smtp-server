package de.gessnerfl.fakesmtp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fakesmtp.webapp.authentication")
public class WebappAuthenticationProperties {
    private String username;
    private String password;

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

    public boolean isAuthenticationEnabled() {
        return username != null && !username.isEmpty() && password != null && !password.isEmpty();
    }
}
