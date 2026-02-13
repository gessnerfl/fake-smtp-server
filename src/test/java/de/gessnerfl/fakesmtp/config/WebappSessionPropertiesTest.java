package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebappSessionPropertiesTest {

    private WebappSessionProperties sut;

    @BeforeEach
    void setUp() {
        sut = new WebappSessionProperties();
    }

    @Test
    void shouldUseDefaultSessionTimeoutMinutes() {
        assertEquals(10, sut.getSessionTimeoutMinutes());
    }

    @Test
    void shouldFallbackToDefaultSessionTimeoutMinutesWhenConfiguredValueIsNonPositive() {
        sut.setSessionTimeoutMinutes(0);

        assertEquals(10, sut.getSessionTimeoutMinutes());
    }

    @Test
    void shouldCapSessionTimeoutMinutesAtMaximum() {
        sut.setSessionTimeoutMinutes(2000);

        assertEquals(1440, sut.getSessionTimeoutMinutes());
    }
}
