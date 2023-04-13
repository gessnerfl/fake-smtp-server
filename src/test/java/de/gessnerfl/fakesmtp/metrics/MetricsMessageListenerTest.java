package de.gessnerfl.fakesmtp.metrics;

import de.gessnerfl.fakesmtp.smtp.server.BlockedRecipientAddresses;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsMessageListenerTest {

	@Mock
	private BlockedRecipientAddresses blockedRecipientAddresses;
	@Spy
	private MeterRegistry registry = new SimpleMeterRegistry();
	@InjectMocks
	private MetricsMessageListener sut;

	@Test
	void shouldIncreaseMessagesBlockedCounterWhenRecipientIsBlocked() {
		final var recipient = "bar";
		when(blockedRecipientAddresses.isBlocked(recipient)).thenReturn(true);

		assertThat(registry.counter(MetricsMessageListener.MESSAGES_BLOCKED,
				"from", "foo", "recipient", recipient)
				.count())
				.isEqualTo(0);
		assertFalse(sut.accept("foo", recipient));
		verify(blockedRecipientAddresses).isBlocked(recipient);
		assertThat(registry.counter(MetricsMessageListener.MESSAGES_BLOCKED,
				"from", "foo", "recipient", recipient)
				.count())
				.isEqualTo(1);

	}

	@Test
	void shouldIncreaseMessagesDeliveredCounterWhenRecipientIsNotBlocked() {
		final var recipient = "bar";
		assertThat(registry.counter(MetricsMessageListener.MESSAGES_DELIVERED,
				"from", "foo", "recipient", recipient)
				.count())
				.isEqualTo(0);
		sut.deliver("foo", recipient, null);
		assertThat(registry.counter(MetricsMessageListener.MESSAGES_DELIVERED,
				"from", "foo", "recipient", recipient)
				.count())
				.isEqualTo(1);

	}
}
