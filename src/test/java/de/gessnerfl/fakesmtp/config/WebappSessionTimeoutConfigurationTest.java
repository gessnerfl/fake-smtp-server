package de.gessnerfl.fakesmtp.config;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebappSessionTimeoutConfigurationTest {

    private final WebappSessionTimeoutConfiguration sut = new WebappSessionTimeoutConfiguration();

    private WebappSessionProperties sessionProperties;

    @BeforeEach
    void setUp() {
        sessionProperties = new WebappSessionProperties();
    }

    @Test
    void shouldSetWebServerSessionTimeoutFromSessionProperties() {
        sessionProperties.setSessionTimeoutMinutes(30);
        ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
        ServletWebServerSettings settings = new ServletWebServerSettings();
        when(factory.getSettings()).thenReturn(settings);

        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> customizer =
                sut.webappSessionTimeoutCustomizer(sessionProperties);

        customizer.customize(factory);

        assertEquals(Duration.ofMinutes(30), settings.getSession().getTimeout());
    }

    @Test
    void shouldApplyMaximumWhenConfiguredSessionTimeoutExceedsCap() {
        sessionProperties.setSessionTimeoutMinutes(2000);
        ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);
        ServletWebServerSettings settings = new ServletWebServerSettings();
        when(factory.getSettings()).thenReturn(settings);

        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> customizer =
                sut.webappSessionTimeoutCustomizer(sessionProperties);

        customizer.customize(factory);

        assertEquals(Duration.ofMinutes(1440), settings.getSession().getTimeout());
    }
}
