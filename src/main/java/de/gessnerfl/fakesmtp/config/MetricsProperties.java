package de.gessnerfl.fakesmtp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fakesmtp.metrics")
public class MetricsProperties {

    private boolean includeAddressTags = false;

    public boolean isIncludeAddressTags() {
        return includeAddressTags;
    }

    public void setIncludeAddressTags(boolean includeAddressTags) {
        this.includeAddressTags = includeAddressTags;
    }
}
