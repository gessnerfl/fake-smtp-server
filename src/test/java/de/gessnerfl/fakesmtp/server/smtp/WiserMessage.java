package de.gessnerfl.fakesmtp.server.smtp;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * This class wraps a received message and provides a way to generate a JavaMail
 * MimeMessage from the data.
 */
public class WiserMessage {
	byte[] messageData;

	Wiser wiser;

	String envelopeSender;

	String envelopeReceiver;

	WiserMessage(final Wiser wiser,
			final String envelopeSender,
			final String envelopeReceiver,
			final byte[] messageData) {
		this.wiser = wiser;
		this.envelopeSender = envelopeSender;
		this.envelopeReceiver = envelopeReceiver;
		this.messageData = messageData;
	}

	/**
	 * Generate a JavaMail MimeMessage.
	 *
	 * @throws MessagingException on messaging error
	 */
	public MimeMessage getMimeMessage() throws MessagingException {
		return new MimeMessage(this.wiser.getSession(), new ByteArrayInputStream(this.messageData));
	}

	/**
	 * Get's the raw message DATA.
	 */
	public byte[] getData() {
		return this.messageData;
	}

	/**
	 * Get's the RCPT TO:
	 */
	public String getEnvelopeReceiver() {
		return this.envelopeReceiver;
	}

	/**
	 * Get's the MAIL FROM:
	 */
	public String getEnvelopeSender() {
		return this.envelopeSender;
	}

	/**
	 * Dumps the rough contents of the message for debugging purposes
	 */
	public void dumpMessage(final PrintStream out) throws MessagingException {
		out.println("===== Dumping message =====");

		out.println("Envelope sender: " + this.getEnvelopeSender());
		out.println("Envelope recipient: " + this.getEnvelopeReceiver());

		// It should all be convertible with ascii or utf8
		final String content = new String(this.getData());
		out.println(content);

		out.println("===== End message dump =====");
	}

	/**
	 * Implementation of toString()
	 *
	 * @return getData() as a string or an empty string if getData is null
	 */
	@Override
	public String toString() {
		if (this.getData() == null) {
			return "";
		}

		return new String(this.getData());
	}
}
