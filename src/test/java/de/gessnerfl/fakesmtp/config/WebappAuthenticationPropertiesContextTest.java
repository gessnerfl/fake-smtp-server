package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class WebappAuthenticationPropertiesContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldFailStartupWhenAuthenticationIsEnabledWithoutCredentials() {
        contextRunner.withPropertyValues(
                "fakesmtp.webapp.authentication.enabled=true",
                "fakesmtp.webapp.authentication.username=testuser"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Web UI authentication requires non-empty username and password when enabled=true"
            );
        });
    }

    @Test
    void shouldFailStartupWhenAuthenticationIsDisabledButCredentialsArePresent() {
        contextRunner.withPropertyValues(
                "fakesmtp.webapp.authentication.enabled=false",
                "fakesmtp.webapp.authentication.username=testuser",
                "fakesmtp.webapp.authentication.password=testpass"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Web UI authentication must not configure username or password when enabled=false"
            );
        });
    }

    @Test
    void shouldLogDeprecationWarningForLegacyImplicitEnablement(CapturedOutput output) {
        contextRunner.withPropertyValues(
                "fakesmtp.webapp.authentication.username=testuser",
                "fakesmtp.webapp.authentication.password=testpass"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(WebappAuthenticationProperties.class).isAuthenticationEnabled()).isTrue();
            assertThat(output).contains("deprecated");
            assertThat(output).contains("fakesmtp.webapp.authentication.enabled");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(WebappAuthenticationProperties.class)
    static class TestConfig {
    }
}
