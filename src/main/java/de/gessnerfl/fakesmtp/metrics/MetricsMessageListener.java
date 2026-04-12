package de.gessnerfl.fakesmtp.metrics;

import de.gessnerfl.fakesmtp.config.MetricsProperties;
import de.gessnerfl.fakesmtp.smtp.server.BlockedRecipientAddresses;
import de.gessnerfl.fakesmtp.smtp.server.MessageListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MetricsMessageListener implements MessageListener {

	public static final String MESSAGES_BLOCKED = "messages.blocked";
	public static final String MESSAGES_DELIVERED = "messages.delivered";
	private final MeterRegistry registry;

	private final BlockedRecipientAddresses blockedRecipientAddresses;
	private final boolean includeAddressTags;

	@Autowired
	public MetricsMessageListener(MeterRegistry registry,
								  BlockedRecipientAddresses blockedRecipientAddresses,
								  MetricsProperties metricsProperties) {
		this(registry, blockedRecipientAddresses, metricsProperties.isIncludeAddressTags());
	}

	MetricsMessageListener(MeterRegistry registry,
						   BlockedRecipientAddresses blockedRecipientAddresses,
						   boolean includeAddressTags) {
		this.registry = registry;
		this.blockedRecipientAddresses = blockedRecipientAddresses;
		this.includeAddressTags = includeAddressTags;
	}

	@Override
	public boolean accept(String from, String recipient) {
		boolean isBlocked = blockedRecipientAddresses.isBlocked(recipient);
		if (isBlocked) {
			counterBuilder(MESSAGES_BLOCKED, from, recipient)
					.register(registry)
					.increment();
		}
		return !isBlocked;

	}

	@Override
	public void deliver(String from, String recipient, InputStream data) {
		counterBuilder(MESSAGES_DELIVERED, from, recipient)
				.register(registry)
				.increment();
	}

	private Counter.Builder counterBuilder(String metricName, String from, String recipient) {
		Counter.Builder builder = Counter.builder(metricName);
		if (includeAddressTags) {
			builder.tag("from", from)
					.tag("recipient", recipient);
		}
		return builder;
	}
}
