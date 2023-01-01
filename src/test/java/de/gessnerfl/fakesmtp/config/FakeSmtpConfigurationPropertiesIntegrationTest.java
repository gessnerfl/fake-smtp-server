package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"mockserver","config_mockserver"})
@ExtendWith(SpringExtension.class)
@SpringBootTest
class FakeSmtpConfigurationPropertiesIntegrationTest {

    @Autowired
    private FakeSmtpConfigurationProperties sut;

    @Test
    void shouldLoadConfigurationParameters() throws Exception {
        assertEquals(1234, sut.getPort().intValue());
        assertEquals(InetAddress.getByName("127.0.0.1"), sut.getBindAddress());
        assertNull(sut.getAuthentication());
        assertNotNull(sut.getPersistence());
        assertEquals(FakeSmtpConfigurationProperties.Persistence.DEFAULT_MAX_NUMBER_EMAILS, sut.getPersistence().getMaxNumberEmails().intValue());
    }
}