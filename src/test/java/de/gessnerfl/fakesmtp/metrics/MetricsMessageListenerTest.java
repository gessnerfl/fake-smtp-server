package de.gessnerfl.fakesmtp.metrics;

import de.gessnerfl.fakesmtp.smtp.server.BlockedRecipientAddresses;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsMessageListenerTest {

    @Mock
    private BlockedRecipientAddresses blockedRecipientAddresses;

    @Test
    void shouldExcludeAddressTagsByDefaultForBlockedMessages() {
        final var recipient = "bar";
        final MeterRegistry registry = new SimpleMeterRegistry();
        final var sut = new MetricsMessageListener(registry, blockedRecipientAddresses, false);
        when(blockedRecipientAddresses.isBlocked(recipient)).thenReturn(true);

        assertFalse(sut.accept("foo", recipient));
        verify(blockedRecipientAddresses).isBlocked(recipient);

        assertThat(registry.find(MetricsMessageListener.MESSAGES_BLOCKED).counter())
                .isNotNull()
                .extracting(counter -> counter.count())
                .isEqualTo(1.0);
        assertThat(registry.find(MetricsMessageListener.MESSAGES_BLOCKED)
                .tags("from", "foo", "recipient", recipient)
                .counter())
                .isNull();
    }

    @Test
    void shouldIncludeAddressTagsWhenExplicitlyEnabledForDeliveredMessages() {
        final var recipient = "bar";
        final MeterRegistry registry = new SimpleMeterRegistry();
        final var sut = new MetricsMessageListener(registry, blockedRecipientAddresses, true);

        sut.deliver("foo", recipient, null);

        assertThat(registry.find(MetricsMessageListener.MESSAGES_DELIVERED)
                .tags("from", "foo", "recipient", recipient)
                .counter())
                .isNotNull()
                .extracting(counter -> counter.count())
                .isEqualTo(1.0);
    }
}
