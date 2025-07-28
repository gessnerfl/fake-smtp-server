package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private FakeSmtpAuthenticationProperties authProperties;

    @InjectMocks
    private SecurityConfig sut;

    @Test
    void shouldCreatePasswordEncoder() {
        PasswordEncoder result = sut.passwordEncoder();

        assertNotNull(result);
        assertTrue(result.getClass().getName().contains("BCrypt"));
    }

    @Test
    void shouldCreateEmptyUserDetailsServiceWhenAuthenticationIsDisabled() {
        when(authProperties.isAuthenticationEnabled()).thenReturn(false);

        UserDetailsService result = sut.userDetailsService();

        assertNotNull(result);
        assertThrows(Exception.class, () -> result.loadUserByUsername("anyuser"));
    }

    @Test
    void shouldCreateUserDetailsServiceWithUserWhenAuthenticationIsEnabled() {
        when(authProperties.isAuthenticationEnabled()).thenReturn(true);
        when(authProperties.getUsername()).thenReturn("testuser");
        when(authProperties.getPassword()).thenReturn("testpass");

        UserDetailsService result = sut.userDetailsService();

        assertNotNull(result);

        UserDetails userDetails = result.loadUserByUsername("testuser");
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
}
