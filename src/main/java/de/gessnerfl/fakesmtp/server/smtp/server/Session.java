package de.gessnerfl.fakesmtp.server.smtp.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandler;
import de.gessnerfl.fakesmtp.server.smtp.DropConnectionException;
import de.gessnerfl.fakesmtp.server.smtp.MessageContext;
import de.gessnerfl.fakesmtp.server.smtp.MessageHandler;
import de.gessnerfl.fakesmtp.server.smtp.io.CRLFTerminatedReader;

/**
 * The thread that handles a connection. This class passes most of it's
 * responsibilities off to the CommandHandler.
 */
public class Session implements Runnable, MessageContext {
	private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);

	/** A link to our parent server */
	private final SMTPServer server;

	/**
	 * A link to our parent server thread, which must be notified when this
	 * connection is finished.
	 */
	private final ServerThread serverThread;

	/**
	 * Saved SLF4J mapped diagnostic context of the parent thread. The parent thread
	 * is the one which calls the constructor. MDC is usually inherited by new
	 * threads, but this mechanism does not work with executors.
	 */
	private final Map<String, String> parentLoggingMdcContext = MDC.getCopyOfContextMap();

	/**
	 * Uniquely identifies this session within an extended time period, useful for
	 * logging.
	 */
	private String sessionId;

	/** Set this true when doing an ordered shutdown */
	private volatile boolean quitting = false;

	/** I/O to the client */
	private Socket socket;

	private InputStream input;

	private CRLFTerminatedReader reader;

	private PrintWriter writer;

	/** Might exist if the client has successfully authenticated */
	private AuthenticationHandler authenticationHandler;

	/**
	 * It exists if a mail transaction is in progress (from the MAIL command up to
	 * the end of the DATA command).
	 */
	private MessageHandler messageHandler;

	/** Some state information */
	private String helo;

	private int recipientCount;

	/**
	 * The recipient address in the first accepted RCPT command, but only if there
	 * is exactly one such accepted recipient. If there is no accepted recipient
	 * yet, or if there are more than one, then this value is null. This information
	 * is useful in the construction of the FOR clause of the Received header.
	 */
	private String singleRecipient;

	/**
	 * If the client told us the size of the message, this is the value. If they
	 * didn't, the value will be 0.
	 */
	private int declaredMessageSize = 0;

	/** Some more state information */
	private boolean tlsStarted;

	private Certificate[] tlsPeerCertificates;

	private boolean updateThreadName = true;

	/**
	 * Creates the Runnable Session object.
	 *
	 * @param server a link to our parent
	 * @param socket is the socket to the client
	 * @throws IOException on IO error
	 */
	public Session(final SMTPServer server, final ServerThread serverThread, final Socket socket) throws IOException {
		this.server = server;
		this.serverThread = serverThread;

		this.setSocket(socket);
	}

	/**
	 * @return a reference to the master server object
	 */
	public SMTPServer getServer() {
		return this.server;
	}

	/**
	 * The thread for each session runs on this and shuts down when the quitting
	 * member goes true.
	 */
	@Override
	public void run() {
		MDC.setContextMap(parentLoggingMdcContext);
		sessionId = server.getSessionIdFactory().create();
		MDC.put("SessionId", sessionId);
		final String originalName;
		if (updateThreadName) {
			originalName = Thread.currentThread().getName();
			Thread.currentThread()
					.setName(Session.class.getName() + "-" + socket.getInetAddress() + ":" + socket.getPort());
		} else {
			originalName = null;
		}

		if (LOGGER.isDebugEnabled()) {
			final InetAddress remoteInetAddress = this.getRemoteAddress().getAddress();
			remoteInetAddress.getHostName(); // Causes future toString() to print the name too

			LOGGER.debug("SMTP connection from {}, new connection count: {}",
					remoteInetAddress,
					this.serverThread.getNumberOfConnections());
		}

		try {
			runCommandLoop();
		} catch (final IOException e1) {
			if (!this.quitting) {
				try {
					// Send a temporary failure back so that the server will try to resend
					// the message later.
					this.sendResponse("421 4.4.0 Problem attempting to execute commands. Please try again later.");
				} catch (final IOException e) {}

				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Exception during SMTP transaction", e1);
				}
			}
		} catch (final Throwable e) {
			LOGGER.error("Unexpected error in the SMTP handler thread", e);
			try {
				this.sendResponse("421 4.3.0 Mail system failure, closing transmission channel");
			} catch (final IOException e1) {
				// just swallow this, the outer exception is the real problem.
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			if (e instanceof Error) {
				throw (Error) e;
			}
			throw new RuntimeException("Unexpected exception", e);
		} finally {
			this.closeConnection();
			this.endMessageHandler();
			serverThread.sessionEnded(this);
			if (updateThreadName) {
				Thread.currentThread().setName(originalName);
			}
			MDC.clear();
		}
	}

	/**
	 * Sends the welcome message and starts receiving and processing client
	 * commands. It quits when {@link #quitting} becomes true or when it can be
	 * noticed or at least assumed that the client no longer sends valid commands,
	 * for example on timeout.
	 *
	 * @throws IOException if sending to or receiving from the client fails.
	 */
	private void runCommandLoop() throws IOException {
		if (this.serverThread.hasTooManyConnections()) {
			LOGGER.debug("SMTP Too many connections!");

			this.sendResponse("421 Too many connections, try again later");
			return;
		}

		this.sendResponse("220 " + this.server.getHostName() + " ESMTP " + this.server.getSoftwareName());

		while (!this.quitting) {
			try {
				String line = null;
				try {
					line = this.reader.readLine();
				} catch (final SocketException ex) {
					// Lots of clients just "hang up" rather than issuing QUIT,
					// which would
					// fill our logs with the warning in the outer catch.
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Error reading client command: " + ex.getMessage(), ex);
					}

					return;
				}

				if (line == null) {
					LOGGER.debug("no more lines from client");
					return;
				}

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Client: " + line);
				}

				this.server.getCommandHandler().handleCommand(this, line);
			} catch (final DropConnectionException ex) {
				this.sendResponse(ex.getErrorResponse());
				return;
			} catch (final SocketTimeoutException ex) {
				this.sendResponse("421 Timeout waiting for data from client.");
				return;
			} catch (final CRLFTerminatedReader.TerminationException te) {
				final String msg = "501 Syntax error at character position "
						+ te.position()
						+ ". CR and LF must be CRLF paired.  See RFC 2821 #2.7.1.";

				LOGGER.debug(msg);
				this.sendResponse(msg);

				// if people are screwing with things, close connection
				return;
			} catch (final CRLFTerminatedReader.MaxLineLengthException mlle) {
				final String msg = "501 " + mlle.getMessage();

				LOGGER.debug(msg);
				this.sendResponse(msg);

				// if people are screwing with things, close connection
				return;
			}
		}
	}

	/**
	 * Close reader, writer, and socket, logging exceptions but otherwise ignoring
	 * them
	 */
	private void closeConnection() {
		try {
			try {
				this.writer.close();
				this.input.close();
			} finally {
				this.closeSocket();
			}
		} catch (final IOException e) {
			LOGGER.info(e.toString());
		}
	}

	/**
	 * Initializes our reader, writer, and the i/o filter chains based on the
	 * specified socket. This is called internally when we startup and when (if) SSL
	 * is started.
	 */
	public void setSocket(final Socket socket) throws IOException {
		this.socket = socket;
		this.input = this.socket.getInputStream();
		this.reader = new CRLFTerminatedReader(this.input, StandardCharsets.US_ASCII);
		this.writer = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.US_ASCII));

		this.socket.setSoTimeout(this.server.getConnectionTimeout());
	}

	/**
	 * This method is only used by the start tls command
	 *
	 * @return the current socket to the client
	 */
	public Socket getSocket() {
		return this.socket;
	}

	/** Close the client socket if it is open */
	public void closeSocket() throws IOException {
		if (this.socket != null && this.socket.isBound() && !this.socket.isClosed()) {
			this.socket.close();
		}
	}

	/**
	 * @return the raw input stream from the client
	 */
	public InputStream getRawInput() {
		return this.input;
	}

	/**
	 * @return the cooked CRLF-terminated reader from the client
	 */
	public CRLFTerminatedReader getReader() {
		return this.reader;
	}

	/** Sends the response to the client */
	public void sendResponse(final String response) throws IOException {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Server: " + response);
		}

		this.writer.print(response + "\r\n");
		this.writer.flush();
	}

	/**
	 * Returns an identifier of the session which is reasonably unique within an
	 * extended time period.
	 */
	public String getSessionId() {
		return sessionId;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.subethamail.smtp.MessageContext#getRemoteAddress()
	 */
	@Override
	public InetSocketAddress getRemoteAddress() {
		return (InetSocketAddress) this.socket.getRemoteSocketAddress();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.subethamail.smtp.MessageContext#getSMTPServer()
	 */
	@Override
	public SMTPServer getSMTPServer() {
		return this.server;
	}

	/**
	 * @return the current message handler
	 */
	public MessageHandler getMessageHandler() {
		return this.messageHandler;
	}

	/** Simple state */
	@Override
	public String getHelo() {
		return this.helo;
	}

	public void setHelo(final String value) {
		this.helo = value;
	}

	/** @deprecated use {@link #isMailTransactionInProgress()} */
	@Deprecated
	public boolean getHasMailFrom() {
		return isMailTransactionInProgress();
	}

	public void addRecipient(final String recipientAddress) {
		this.recipientCount++;
		this.singleRecipient = this.recipientCount == 1 ? recipientAddress : null;
	}

	public int getRecipientCount() {
		return this.recipientCount;
	}

	/**
	 * Returns the first accepted recipient if there is exactly one accepted
	 * recipient, otherwise it returns null.
	 */
	public String getSingleRecipient() {
		return singleRecipient;
	}

	public boolean isAuthenticated() {
		return this.authenticationHandler != null;
	}

	@Override
	public AuthenticationHandler getAuthenticationHandler() {
		return this.authenticationHandler;
	}

	/**
	 * This is called by the AuthCommand when a session is successfully
	 * authenticated. The handler will be an object created by the
	 * AuthenticationHandlerFactory.
	 */
	public void setAuthenticationHandler(final AuthenticationHandler handler) {
		this.authenticationHandler = handler;
	}

	/**
	 * @return the maxMessageSize
	 */
	public int getDeclaredMessageSize() {
		return this.declaredMessageSize;
	}

	/**
	 * @param declaredMessageSize the size that the client says the message will be
	 */
	public void setDeclaredMessageSize(final int declaredMessageSize) {
		this.declaredMessageSize = declaredMessageSize;
	}

	/**
	 * Starts a mail transaction by creating a new message handler.
	 *
	 * @throws IllegalStateException if a mail transaction is already in progress
	 */
	public void startMailTransaction() throws IllegalStateException {
		if (this.messageHandler != null) {
			throw new IllegalStateException("Mail transaction is already in progress");
		}
		this.messageHandler = this.server.getMessageHandlerFactory().create(this);
	}

	/**
	 * Returns true if a mail transaction is started, i.e. a MAIL command is
	 * received, and the transaction is not yet completed or aborted. A transaction
	 * is successfully completed after the message content is received and accepted
	 * at the end of the DATA command.
	 */
	public boolean isMailTransactionInProgress() {
		return this.messageHandler != null;
	}

	/**
	 * Stops the mail transaction if it in progress and resets all state related to
	 * mail transactions.
	 * <p>
	 * Note: Some state is associated with each particular message (senders,
	 * recipients, the message handler).<br>
	 * Some state is not; seeing hello, TLS, authentication.
	 */
	public void resetMailTransaction() {
		this.endMessageHandler();
		this.messageHandler = null;
		this.recipientCount = 0;
		this.singleRecipient = null;
		this.declaredMessageSize = 0;
	}

	/** @deprecated use {@link #resetMailTransaction()} */
	@Deprecated
	public void resetMessageState() {
		resetMailTransaction();
	}

	/** Safely calls done() on a message hander, if one exists */
	private void endMessageHandler() {
		if (this.messageHandler != null) {
			try {
				this.messageHandler.done();
			} catch (final Throwable ex) {
				LOGGER.error("done() threw exception", ex);
			}
		}
	}

	/**
	 * Reset the SMTP protocol to the initial state, which is the state after a
	 * server issues a 220 service ready greeting.
	 */
	public void resetSmtpProtocol() {
		resetMailTransaction();
		this.helo = null;
	}

	/**
	 * Triggers the shutdown of the thread and the closing of the connection.
	 */
	public void quit() {
		this.quitting = true;
		this.closeConnection();
	}

	/**
	 * @return true when the TLS handshake was completed, false otherwise
	 */
	public boolean isTLSStarted() {
		return tlsStarted;
	}

	/**
	 * @param tlsStarted true when the TLS handshake was completed, false otherwise
	 */
	public void setTlsStarted(final boolean tlsStarted) {
		this.tlsStarted = tlsStarted;
	}

	public void setTlsPeerCertificates(final Certificate[] tlsPeerCertificates) {
		this.tlsPeerCertificates = tlsPeerCertificates;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Certificate[] getTlsPeerCertificates() {
		return tlsPeerCertificates;
	}

	public boolean isUpdateThreadName() {
		return updateThreadName;
	}

	public void setUpdateThreadName(final boolean updateThreadName) {
		this.updateThreadName = updateThreadName;
	}
}
