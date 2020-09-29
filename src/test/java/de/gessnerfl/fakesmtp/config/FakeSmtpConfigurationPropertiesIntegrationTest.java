package de.gessnerfl.fakesmtp.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.InetAddress;

import static org.junit.Assert.*;

@ActiveProfiles({"integrationtest","config_integrationtest"})
@RunWith(SpringRunner.class)
@SpringBootTest
public class FakeSmtpConfigurationPropertiesIntegrationTest {

    @Autowired
    private FakeSmtpConfigurationProperties sut;

    @Test
    public void shouldLoadConfigurationParameters() throws Exception {
        assertEquals(1234, sut.getPort().intValue());
        assertEquals(InetAddress.getByName("127.0.0.1"), sut.getBindAddress());
        assertNull(sut.getAuthentication());
        assertNotNull(sut.getPersistence());
        assertEquals(FakeSmtpConfigurationProperties.Persistence.DEFAULT_MAX_NUMBER_EMAILS, sut.getPersistence().getMaxNumberEmails().intValue());
    }
}