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
}
