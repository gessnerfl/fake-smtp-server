package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FakeSmtpAuthenticationPropertiesTest {

    private FakeSmtpAuthenticationProperties sut;

    @BeforeEach
    void setUp() {
        sut = new FakeSmtpAuthenticationProperties();
    }

    @Test
    void shouldReturnTrueWhenUsernameAndPasswordAreSet() {
        sut.setUsername("testuser");
        sut.setPassword("testpass");

        assertTrue(sut.isAuthenticationEnabled());
        assertEquals("testuser", sut.getUsername());
        assertEquals("testpass", sut.getPassword());
    }

    @Test
    void shouldReturnFalseWhenUsernameIsNull() {
        sut.setUsername(null);
        sut.setPassword("testpass");

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnFalseWhenUsernameIsEmpty() {
        sut.setUsername("");
        sut.setPassword("testpass");

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnFalseWhenPasswordIsNull() {
        sut.setUsername("testuser");
        sut.setPassword(null);

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnFalseWhenPasswordIsEmpty() {
        sut.setUsername("testuser");
        sut.setPassword("");

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnFalseWhenBothUsernameAndPasswordAreNull() {
        sut.setUsername(null);
        sut.setPassword(null);

        assertFalse(sut.isAuthenticationEnabled());
    }

    @Test
    void shouldReturnFalseWhenBothUsernameAndPasswordAreEmpty() {
        sut.setUsername("");
        sut.setPassword("");

        assertFalse(sut.isAuthenticationEnabled());
    }
}
