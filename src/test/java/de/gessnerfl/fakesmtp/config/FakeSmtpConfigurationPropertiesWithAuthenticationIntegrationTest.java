package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles({"mockserver", "config_with_auth_mockserver"})
@ExtendWith(SpringExtension.class)
@SpringBootTest
class FakeSmtpConfigurationPropertiesWithAuthenticationIntegrationTest {

    @Autowired
    private FakeSmtpConfigurationProperties sut;

    @Test
    void shouldLoadConfigurationParameters() throws Exception {
        Assertions.assertEquals(1234, sut.getPort().intValue());
        Assertions.assertEquals(InetAddress.getByName("127.0.0.1"), sut.getBindAddress());
        Assertions.assertNotNull(sut.getAuthentication());
        Assertions.assertEquals("user", sut.getAuthentication().getUsername());
        Assertions.assertEquals("password", sut.getAuthentication().getPassword());
        Assertions.assertNotNull(sut.getPersistence());
        assertNotNull(sut.getPersistence().getEmailDataRetentionTimer());
        assertEquals(FakeSmtpConfigurationProperties.Persistence.DEFAULT_MAX_NUMBER_EMAILS, sut.getPersistence().getMaxNumberEmails());
        assertEquals(FakeSmtpConfigurationProperties.FixedDelayTimerSettings.DEFAULT_FIXED_DELAY, sut.getPersistence().getEmailDataRetentionTimer().getFixedDelay());
        assertEquals(FakeSmtpConfigurationProperties.FixedDelayTimerSettings.DEFAULT_INITIAL_DELAY, sut.getPersistence().getEmailDataRetentionTimer().getInitialDelay());
    }
}