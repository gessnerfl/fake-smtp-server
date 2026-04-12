package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebappAuthenticationPropertiesTest {

    private WebappAuthenticationProperties sut;

    @BeforeEach
    void setup() {
        sut = new WebappAuthenticationProperties();
    }

    @Test
    void shouldReturnFalseForIsAuthenticationEnabledWhenUsernameIsNull() {
        sut.setUsername(null);
        sut.setPassword("password");

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnFalseForIsAuthenticationEnabledWhenUsernameIsEmpty() {
        sut.setUsername("");
        sut.setPassword("password");

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnFalseForIsAuthenticationEnabledWhenPasswordIsNull() {
        sut.setUsername("username");
        sut.setPassword(null);

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnFalseForIsAuthenticationEnabledWhenPasswordIsEmpty() {
        sut.setUsername("username");
        sut.setPassword("");

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnTrueForIsAuthenticationEnabledWhenUsernameAndPasswordAreProvided() {
        sut.setUsername("username");
        sut.setPassword("password");

        assertTrue(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnTrueWhenAuthenticationIsExplicitlyEnabledAndCredentialsAreProvided() {
        sut.setEnabled(true);
        sut.setUsername("username");
        sut.setPassword("password");

        assertTrue(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnFalseWhenAuthenticationIsExplicitlyDisabledAndCredentialsAreMissing() {
        sut.setEnabled(false);

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldValidateLegacyCompatibilityPathWhenEnabledFlagIsUnsetAndCredentialsAreProvided() {
        sut.setUsername("username");
        sut.setPassword("password");

        assertDoesNotThrow(() -> sut.validateConfiguration());
        assertTrue(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldRejectPartialCredentialsWhenEnabledFlagIsUnset() {
        sut.setUsername("username");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> sut.validateConfiguration());

        assertTrue(exception.getMessage().contains("Both username and password must be configured together"));
    }

    @Test
    void shouldRejectEnabledAuthenticationWithoutCredentials() {
        sut.setEnabled(true);
        sut.setUsername("username");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> sut.validateConfiguration());

        assertTrue(exception.getMessage().contains("requires non-empty username and password"));
    }

    @Test
    void shouldRejectDisabledAuthenticationWhenCredentialsArePresent() {
        sut.setEnabled(false);
        sut.setUsername("username");
        sut.setPassword("password");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> sut.validateConfiguration());

        assertTrue(exception.getMessage().contains("must not configure username or password"));
    }

    @Test
    void shouldReturnDefaultValueForConcurrentSessions() {
        assertEquals(1, sut.getConcurrentSessions());
    }

    @Test
    void shouldAllowSettingConcurrentSessions() {
        sut.setConcurrentSessions(5);

        assertEquals(5, sut.getConcurrentSessions());
    }

    @Test
    void shouldAllowSettingUnlimitedConcurrentSessions() {
        sut.setConcurrentSessions(-1);

        assertEquals(-1, sut.getConcurrentSessions());
    }
}
