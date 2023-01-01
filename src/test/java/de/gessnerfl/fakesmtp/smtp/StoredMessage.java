package de.gessnerfl.fakesmtp.smtp;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

public class StoredMessage {
	private final byte[] messageData;
	private final String sender;
	private final String receiver;

	StoredMessage(final String sender, final String receiver, final byte[] messageData) {
		this.sender = sender;
		this.receiver = receiver;
		this.messageData = messageData;
	}

	public MimeMessage getMimeMessage() throws MessagingException {
		final var session = Session.getDefaultInstance(new Properties());
		return new MimeMessage(session, new ByteArrayInputStream(this.messageData));
	}

	public byte[] getData() {
		return this.messageData;
	}

	public String getReceiver() {
		return this.receiver;
	}

	public String getSender() {
		return this.sender;
	}

	@Override
	public String toString() {
		return messageData == null ? "" : new String(this.getData());
	}
}
