package de.gessnerfl.fakesmtp.smtp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.gessnerfl.fakesmtp.smtp.server.MessageListener;
import de.gessnerfl.fakesmtp.smtp.server.MessageListenerAdapter;
import de.gessnerfl.fakesmtp.smtp.server.BaseSmtpServer;

/**
 * Wiser is a tool for unit testing applications that send mail. Your unit tests
 * can start Wiser, run tests which generate emails, then examine the emails
 * that Wiser received and verify their integrity.
 *
 * Wiser is not intended to be a "real" mail server and is not adequate for that
 * purpose; it simply stores all mail in memory. Use the MessageHandlerFactory
 * interface (optionally with the SimpleMessageListenerAdapter) instead.
 */
public class Wiser implements MessageListener {
	private final static Logger log = LoggerFactory.getLogger(Wiser.class);

	BaseSmtpServer server;

	protected List<WiserMessage> messages = Collections.synchronizedList(new ArrayList<WiserMessage>());

	/**
	 * Create a new SMTP server with this class as the listener. The default port is
	 * 25. Call setPort()/setHostname() before calling start().
	 */
	public Wiser() {
		this.server = new BaseSmtpServer("FakeSMTPServer", new MessageListenerAdapter(this));
	}

	/**
	 * Convenience constructor
	 *
	 * @param port the port to listen on
	 */
	public Wiser(final int port) {
		this();
		this.setPort(port);
	}

	/**
	 * The port that the server should listen on.
	 *
	 * @param port the port to listen on
	 */
	public void setPort(final int port) {
		this.server.setPort(port);
	}

	/**
	 * The hostname that the server should listen on.
	 *
	 * @param hostname the hostname to listen on
	 */
	public void setHostname(final String hostname) {
		this.server.setHostName(hostname);
	}

	/** Starts the SMTP Server */
	public void start() {
		this.server.start();
	}

	/** Stops the SMTP Server */
	public void stop() {
		this.server.stop();
	}

	/**
	 * A main() for this class. Starts up the server.
	 *
	 * @param args command line arguments
	 * @throws Exception on error
	 */
	public static void main(final String[] args) throws Exception {
		final Wiser wiser = new Wiser();
		wiser.start();
	}

	/** Always accept everything */
	@Override
	public boolean accept(final String from, final String recipient) {
		if (log.isDebugEnabled()) {
			log.debug("Accepting mail from " + from + " to " + recipient);
		}

		return true;
	}

	/** Cache the messages in memory */
	@Override
	public void deliver(final String from, final String recipient, InputStream data)
			throws TooMuchDataException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Delivering mail from " + from + " to " + recipient);
		}

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		data = new BufferedInputStream(data);

		// read the data from the stream
		int current;
		while ((current = data.read()) >= 0) {
			out.write(current);
		}

		final byte[] bytes = out.toByteArray();

		if (log.isDebugEnabled()) {
			log.debug("Creating message from data with " + bytes.length + " bytes");
		}

		// create a new WiserMessage.
		this.messages.add(new WiserMessage(this, from, recipient, bytes));
	}

	/**
	 * Creates the JavaMail Session object for use in WiserMessage
	 *
	 * @return the JavaMail session
	 */
	protected Session getSession() {
		return Session.getDefaultInstance(new Properties());
	}

	/**
	 * Returns the list of WiserMessages.
	 *
	 * <p>
	 * The number of mail transactions and the number of mails may be different. If
	 * a message is received with multiple recipients in a single mail transaction,
	 * then the list will contain more WiserMessage instances, one for each
	 * recipient.
	 *
	 * @return the list of WiserMessages
	 */
	public List<WiserMessage> getMessages() {
		return this.messages;
	}

	/**
	 * Returns the server implementation
	 *
	 * @return the server implementation
	 */
	public BaseSmtpServer getServer() {
		return this.server;
	}

	/**
	 * For debugging purposes, dumps a rough outline of the messages to the output
	 * stream.
	 *
	 * @param out the output PrintStream
	 * @throws MessagingException on dump error
	 */
	public void dumpMessages(final PrintStream out) throws MessagingException {
		out.println("----- Start printing messages -----");

		for (final WiserMessage wmsg : this.getMessages()) {
			wmsg.dumpMessage(out);
		}

		out.println("----- End printing messages -----");
	}
}
