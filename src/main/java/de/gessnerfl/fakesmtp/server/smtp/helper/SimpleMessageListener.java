package de.gessnerfl.fakesmtp.server.smtp.helper;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is an interface for processing the end-result messages that is
 * higher-level than the MessageHandler and related factory.
 *
 * While the SMTP message is being received, all listeners are asked if they
 * want to accept each recipient. After the message has arrived, the message is
 * handed off to all accepting listeners.
 */
public interface SimpleMessageListener {
	/**
	 * Called once for every RCPT TO during a SMTP exchange. Each accepted recipient
	 * will result in a separate deliver() call later.
	 *
	 * @param from      is a rfc822-compliant email address.
	 * @param recipient is a rfc822-compliant email address.
	 *
	 * @return true if the listener wants delivery of the message, false if the
	 *         message is not for this listener.
	 */
	boolean accept(String from, String recipient);

	/**
	 * When message data arrives, this method will be called for every recipient
	 * this listener accepted.
	 *
	 * @param from      is the envelope sender in rfc822 form
	 * @param recipient will be an accepted recipient in rfc822 form
	 * @param data      will be the smtp data stream, stripped of any extra '.'
	 *                  chars. The data stream is only valid for the duration of
	 *                  this call.
	 *
	 * @throws IOException          if there is an IO error reading the input data.
	 */
	void deliver(String from, String recipient, InputStream data) throws IOException;
}
