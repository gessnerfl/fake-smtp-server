package de.gessnerfl.fakesmtp.server.smtp.server;

import java.util.Locale;

/**
 * TimeBasedSessionIdFactory is a very simple {@link SessionIdFactory}, which
 * assigns numeric identifiers based on the current milliseconds time, amending
 * it as necessary to make it unique.
 */
public class TimeBasedSessionIdFactory implements SessionIdFactory {
	private long lastAllocatedId = 0;

	@Override
	public String create() {
		long id = System.currentTimeMillis();
		synchronized (this) {
			if (id <= lastAllocatedId) {
				id = lastAllocatedId + 1;
			}
			lastAllocatedId = id;
		}
		return Long.toString(id, 36).toUpperCase(Locale.ENGLISH);
	}
}
