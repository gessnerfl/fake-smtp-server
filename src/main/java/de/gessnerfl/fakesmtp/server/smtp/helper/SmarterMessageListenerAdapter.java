/*
 * $Id: SimpleMessageListenerAdapter.java 320 2009-05-20 09:19:20Z lhoriman $
 * $URL:
 * https://subethasmtp.googlecode.com/svn/trunk/src/org/subethamail/smtp/helper/
 * SimpleMessageListenerAdapter.java $
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
import de.gessnerfl.fakesmtp.server.smtp.helper.SmarterMessageListener.Receiver;
import de.gessnerfl.fakesmtp.server.smtp.io.DeferredFileOutputStream;

/**
 * MessageHandlerFactory implementation which adapts to a collection of
 * SmarterMessageListeners. This is actually half-way between the
 * SimpleMessageListener interface and the raw MessageHandler.
 *
 * The key point is that for any message, every accepted recipient will get a
 * separate delivery.
 *
 * @author Jeff Schnitzer
 */
public class SmarterMessageListenerAdapter implements MessageHandlerFactory {
	/**
	 * 5 megs by default. The server will buffer incoming messages to disk when they
	 * hit this limit in the DATA received.
	 */
	private static int DEFAULT_DATA_DEFERRED_SIZE = 1024 * 1024 * 5;

	private final Collection<SmarterMessageListener> listeners;

	private final int dataDeferredSize;

	/**
	 * Initializes this factory with a single listener.
	 *
	 * Default data deferred size is 5 megs.
	 */
	public SmarterMessageListenerAdapter(final SmarterMessageListener listener) {
		this(Collections.singleton(listener), DEFAULT_DATA_DEFERRED_SIZE);
	}

	/**
	 * Initializes this factory with the listeners.
	 *
	 * Default data deferred size is 5 megs.
	 */
	public SmarterMessageListenerAdapter(final Collection<SmarterMessageListener> listeners) {
		this(listeners, DEFAULT_DATA_DEFERRED_SIZE);
	}

	/**
	 * Initializes this factory with the listeners.
	 *
	 * @param dataDeferredSize The server will buffer incoming messages to disk when
	 *                         they hit this limit in the DATA received.
	 */
	public SmarterMessageListenerAdapter(final Collection<SmarterMessageListener> listeners,
			final int dataDeferredSize) {
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
	 * Class which implements the actual handler interface.
	 */
	class Handler implements MessageHandler {
		MessageContext ctx;

		String from;

		List<Receiver> deliveries = new ArrayList<>();

		public Handler(final MessageContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void from(final String from) throws RejectException {
			this.from = from;
		}

		@Override
		public void recipient(final String recipient) throws RejectException {
			for (final SmarterMessageListener listener : SmarterMessageListenerAdapter.this.listeners) {
				final Receiver rec = listener.accept(this.from, recipient);

				if (rec != null) {
					this.deliveries.add(rec);
				}
			}

			if (this.deliveries.isEmpty()) {
				throw new RejectException(553, "<" + recipient + "> address unknown.");
			}
		}

		@Override
		public void data(final InputStream data) throws TooMuchDataException, IOException {
			if (this.deliveries.size() == 1) {
				this.deliveries.get(0).deliver(data);
			} else {
				try (DeferredFileOutputStream dfos
						= new DeferredFileOutputStream(SmarterMessageListenerAdapter.this.dataDeferredSize)) {
					int value;
					while ((value = data.read()) >= 0) {
						dfos.write(value);
					}

					for (final Receiver rec : this.deliveries) {
						rec.deliver(dfos.getInputStream());
					}
				}
			}
		}

		@Override
		public void done() {
			for (final Receiver rec : this.deliveries) {
				rec.done();
			}
		}
	}
}
