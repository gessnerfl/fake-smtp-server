package de.gessnerfl.fakesmtp.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.InetAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@ActiveProfiles("integrationtest,config_with_auth_integrationtest")
@RunWith(SpringRunner.class)
@SpringBootTest
public class FakeSmtpConfigurationPropertiesWithAuthenticationIntegrationTest {

    @Autowired
    private FakeSmtpConfigurationProperties sut;

    @Test
    public void shouldLoadConfigurationParameters() throws Exception {
        assertEquals(1234, sut.getPort().intValue());
        assertEquals(InetAddress.getByName("127.0.0.1"), sut.getBindAddress());
        assertNotNull(sut.getAuthentication());
        assertEquals("user", sut.getAuthentication().getUsername());
        assertEquals("password", sut.getAuthentication().getPassword());
        assertNotNull(sut.getPersistence());
        assertEquals(FakeSmtpConfigurationProperties.Persistence.DEFAULT_MAX_NUMBER_EMAILS, sut.getPersistence().getMaxNumberEmails().intValue());
    }
}