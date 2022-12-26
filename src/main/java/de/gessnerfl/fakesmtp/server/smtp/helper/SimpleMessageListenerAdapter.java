/*
 * $Id$ $URL$
 */
package de.gessnerfl.fakesmtp.server.smtp.helper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.gessnerfl.fakesmtp.server.smtp.MessageContext;
import de.gessnerfl.fakesmtp.server.smtp.MessageHandler;
import de.gessnerfl.fakesmtp.server.smtp.MessageHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.RejectException;
import de.gessnerfl.fakesmtp.server.smtp.TooMuchDataException;
import de.gessnerfl.fakesmtp.server.smtp.io.DeferredFileOutputStream;

/**
 * MessageHandlerFactory implementation which adapts to a collection of
 * MessageListeners. This allows us to preserve the old, convenient interface.
 *
 * @author Jeff Schnitzer
 */
public class SimpleMessageListenerAdapter implements MessageHandlerFactory {
	/**
	 * 5 megs by default. The server will buffer incoming messages to disk when they
	 * hit this limit in the DATA received.
	 */
	private static int DEFAULT_DATA_DEFERRED_SIZE = 1024 * 1024 * 5;

	private final Collection<SimpleMessageListener> listeners;

	private final int dataDeferredSize;

	/**
	 * Initializes this factory with a single listener.
	 *
	 * Default data deferred size is 5 megs.
	 */
	public SimpleMessageListenerAdapter(final SimpleMessageListener listener) {
		this(Collections.singleton(listener), DEFAULT_DATA_DEFERRED_SIZE);
	}

	/**
	 * Initializes this factory with the listeners.
	 *
	 * Default data deferred size is 5 megs.
	 */
	public SimpleMessageListenerAdapter(final Collection<SimpleMessageListener> listeners) {
		this(listeners, DEFAULT_DATA_DEFERRED_SIZE);
	}

	/**
	 * Initializes this factory with the listeners.
	 *
	 * @param dataDeferredSize The server will buffer incoming messages to disk when
	 *                         they hit this limit in the DATA received.
	 */
	public SimpleMessageListenerAdapter(final Collection<SimpleMessageListener> listeners, final int dataDeferredSize) {
		this.listeners = listeners;
		this.dataDeferredSize = dataDeferredSize;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.subethamail.smtp.MessageHandlerFactory#create(org.subethamail.smtp.
	 * MessageContext)
	 */
	@Override
	public MessageHandler create(final MessageContext ctx) {
		return new Handler(ctx);
	}

	/**
	 * Needed by this class to track which listeners need delivery.
	 */
	static class Delivery {
		SimpleMessageListener listener;

		public SimpleMessageListener getListener() {
			return this.listener;
		}

		String recipient;

		public String getRecipient() {
			return this.recipient;
		}

		public Delivery(final SimpleMessageListener listener, final String recipient) {
			this.listener = listener;
			this.recipient = recipient;
		}
	}

	/**
	 * Class which implements the actual handler interface.
	 */
	class Handler implements MessageHandler {
		MessageContext ctx;

		String from;

		List<Delivery> deliveries = new ArrayList<>();

		public Handler(final MessageContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void from(final String from) throws RejectException {
			this.from = from;
		}

		@Override
		public void recipient(final String recipient) throws RejectException {
			boolean addedListener = false;

			for (final SimpleMessageListener listener : SimpleMessageListenerAdapter.this.listeners) {
				if (listener.accept(this.from, recipient)) {
					this.deliveries.add(new Delivery(listener, recipient));
					addedListener = true;
				}
			}

			if (!addedListener) {
				throw new RejectException(553, "<" + recipient + "> address unknown.");
			}
		}

		@Override
		public void data(final InputStream data) throws IOException {
			if (this.deliveries.size() == 1) {
				final Delivery delivery = this.deliveries.get(0);
				delivery.getListener().deliver(this.from, delivery.getRecipient(), data);
			} else {
				try (DeferredFileOutputStream dfos
						= new DeferredFileOutputStream(SimpleMessageListenerAdapter.this.dataDeferredSize)) {
					int value;
					while ((value = data.read()) >= 0) {
						dfos.write(value);
					}

					for (final Delivery delivery : this.deliveries) {
						delivery.getListener().deliver(this.from, delivery.getRecipient(), dfos.getInputStream());
					}
				}
			}
		}

		@Override
		public void done() {}
	}
}
