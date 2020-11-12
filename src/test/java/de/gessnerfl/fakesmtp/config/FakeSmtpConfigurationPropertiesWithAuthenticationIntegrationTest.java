package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetAddress;

@ActiveProfiles({"integrationtest", "config_with_auth_integrationtest"})
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
        Assertions.assertEquals(FakeSmtpConfigurationProperties.Persistence.DEFAULT_MAX_NUMBER_EMAILS, sut.getPersistence().getMaxNumberEmails().intValue());
    }
}