package de.gessnerfl.fakesmtp.config;

import java.time.Duration;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebappSessionTimeoutConfiguration {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webappSessionTimeoutCustomizer(
            WebappSessionProperties sessionProperties) {
        return factory -> factory.getSettings()
                .getSession()
                .setTimeout(Duration.ofMinutes(sessionProperties.getSessionTimeoutMinutes()));
    }
}
