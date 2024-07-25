package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetAddress;

@ActiveProfiles({"mockserver","config_with_persistence_mockserver"})
@ExtendWith(SpringExtension.class)
@SpringBootTest
class FakeSmtpConfigurationPropertiesWithPersistenceIntegrationTest {

    @Autowired
    private FakeSmtpConfigurationProperties sut;

    @Test
    void shouldLoadConfigurationParameters() throws Exception {
        Assertions.assertEquals(1234, sut.getPort().intValue());
        Assertions.assertEquals(InetAddress.getByName("127.0.0.1"), sut.getBindAddress());
        Assertions.assertNull(sut.getAuthentication());
        Assertions.assertNotNull(sut.getPersistence());
        Assertions.assertEquals(5, sut.getPersistence().getMaxNumberEmails().intValue());
        Assertions.assertEquals(100000, sut.getPersistence().getFixedDelay().intValue());
        Assertions.assertEquals(100000, sut.getPersistence().getInitialDelay().intValue());
    }
}
