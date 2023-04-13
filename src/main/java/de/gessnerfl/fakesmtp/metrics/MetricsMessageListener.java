package de.gessnerfl.fakesmtp.metrics;

import de.gessnerfl.fakesmtp.smtp.server.BlockedRecipientAddresses;
import de.gessnerfl.fakesmtp.smtp.server.MessageListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MetricsMessageListener implements MessageListener {


	public static final String MESSAGES_BLOCKED = "messages.blocked";
	public static final String MESSAGES_DELIVERED = "messages.delivered";
	private MeterRegistry registry;

	private BlockedRecipientAddresses blockedRecipientAddresses;

	public MetricsMessageListener(MeterRegistry registry, BlockedRecipientAddresses blockedRecipientAddresses) {
		this.registry = registry;
		this.blockedRecipientAddresses = blockedRecipientAddresses;
	}

	@Override
	public boolean accept(String from, String recipient) {
		boolean isBlocked = blockedRecipientAddresses.isBlocked(recipient);
		if (isBlocked) {
			Counter.builder(MESSAGES_BLOCKED)
					.tag("from", from)
					.tag("recipient", recipient)
					.register(registry)
					.increment();
		}
		return !isBlocked;

	}

	@Override
	public void deliver(String from, String recipient, InputStream data) {
		Counter.builder(MESSAGES_DELIVERED)
				.tag("from", from)
				.tag("recipient", recipient)
				.register(registry)
				.increment();
	}
}
