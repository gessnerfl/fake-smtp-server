package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlockedRecipientAddressesTest {

    BlockedRecipientAddresses sut;

    @BeforeEach
    void init(){
        var blokedAddresses = Collections.singletonList("Blocked@Example.Com");
        var properties = mock(FakeSmtpConfigurationProperties.class);
        when(properties.getBlockedRecipientAddresses()).thenReturn(blokedAddresses);
        sut = new BlockedRecipientAddresses(properties);
    }

    @Test
    public void shouldNormalizeEmailsToLowerCaseOnInit(){
        MatcherAssert.assertThat(sut.blockedRecipientAddresses, Matchers.contains("blocked@example.com"));
    }

    @Test
    public void shouldReturnTrueWhenRecipientIsBlockedCaseInsensitive(){
        assertTrue(sut.isBlocked("Blocked@Example.Com"));
        assertTrue(sut.isBlocked("blocked@example.Com"));
    }

    @Test
    public void shouldReturnFalseWehnRecipientIsNull(){
        assertFalse(sut.isBlocked(null));
    }

    @Test
    public void shouldReturnFalseWehnRecipientIsNotBlocked(){
        assertFalse(sut.isBlocked("valid@example.com"));
    }
}