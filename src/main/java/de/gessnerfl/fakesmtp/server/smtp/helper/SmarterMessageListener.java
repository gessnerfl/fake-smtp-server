/*
 * $Id: SimpleMessageListener.java 308 2009-05-06 02:25:23Z lhoriman $ $URL:
 * https://subethasmtp.googlecode.com/svn/trunk/src/org/subethamail/smtp/helper/
 * SimpleMessageListener.java $
 */
package de.gessnerfl.fakesmtp.server.smtp.helper;

import java.io.IOException;
import java.io.InputStream;

import de.gessnerfl.fakesmtp.server.smtp.TooMuchDataException;

/**
 * This is an interface for processing the end-result messages that is
 * higher-level than the MessageHandler and related factory but lower-level than
 * SimpleMessageListener.
 *
 * @author Jeff Schnitzer
 */
public interface SmarterMessageListener {
	/**
	 * Called once for every RCPT TO during a SMTP exchange. Each accepted recipient
	 * returns a Receiver which will have its deliver() mehtod called later.
	 *
	 * @param from      is a rfc822-compliant email address.
	 * @param recipient is a rfc822-compliant email address.
	 *
	 * @return A Receiver if the listener wants delivery of the message, null if the
	 *         message is not to be accepted.
	 */
	Receiver accept(String from, String recipient);

	/**
	 * Interface which accepts delivery of a message.
	 */
	public interface Receiver {
		/**
		 * When message data arrives, this method will be called for every recipient
		 * this listener accepted.
		 *
		 * @param data will be the smtp data stream, stripped of any extra '.' chars.
		 *             The data stream is only valid for the duration of this call.
		 *
		 * @throws TooMuchDataException if the listener can't handle that much data. An
		 *                              error will be reported to the client.
		 * @throws IOException          if there is an IO error reading the input data.
		 */
		void deliver(InputStream data) throws TooMuchDataException, IOException;

		/**
		 * Called at the end of the SMTP exchange, even if no data was delivered.
		 */
		void done();
	}
}
