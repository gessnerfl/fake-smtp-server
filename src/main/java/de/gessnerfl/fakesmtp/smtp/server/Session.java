package de.gessnerfl.fakesmtp.smtp.server;

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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import de.gessnerfl.fakesmtp.smtp.AuthenticationHandler;
import de.gessnerfl.fakesmtp.smtp.MessageContext;
import de.gessnerfl.fakesmtp.smtp.MessageHandler;
import de.gessnerfl.fakesmtp.smtp.io.CRLFTerminatedReader;

/**
 * The thread that handles a connection. This class passes most of it's
 * responsibilities off to the CommandHandler.
 */
public class Session implements Runnable, MessageContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);
    /**
     * The timeout for waiting for data on a connection is one minute: 1000 * 60 * 1
     */
    private static final int CONNECTION_TIMEOUT = 1000 * 60;

    /**
     * A link to our parent server
     */
    private final BaseSmtpServer server;

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

    /**
     * Set this true when doing an ordered shutdown
     */
    private volatile boolean quitting = false;

    /**
     * I/O to the client
     */
    private Socket socket;

    private InputStream input;

    private CRLFTerminatedReader reader;

    private PrintWriter writer;

    /**
     * Might exist if the client has successfully authenticated
     */
    private AuthenticationHandler authenticationHandler;

    /**
     * It exists if a mail transaction is in progress (from the MAIL command up to
     * the end of the DATA command).
     */
    private MessageHandler messageHandler;

    /**
     * Some state information
     */
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
     * Some more state information
     */
    private boolean tlsStarted;

    private Certificate[] tlsPeerCertificates;

    /**
     * Creates the Runnable Session object.
     *
     * @param server a link to our parent
     * @param socket is the socket to the client
     * @throws IOException on IO error
     */
    public Session(final BaseSmtpServer server, final ServerThread serverThread, final Socket socket) throws IOException {
        this.server = server;
        this.serverThread = serverThread;

        this.setSocket(socket);
    }

    /**
     * @return a reference to the master server object
     */
    public BaseSmtpServer getServer() {
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
        originalName = Thread.currentThread().getName();
        Thread.currentThread()
                .setName(Session.class.getName() + "-" + socket.getInetAddress() + ":" + socket.getPort());

        logDebugDetailsOnRun();

        try {
            runCommandLoop();
        } catch (final IOException e1) {
            handleIOExceptionOnRun(e1);
        } catch (final Exception e) {
            handleExceptionDuringRun(e);
        } finally {
            onRunCompleted(originalName);
        }
    }

    private void logDebugDetailsOnRun() {
        if (LOGGER.isDebugEnabled()) {
            final InetAddress remoteInetAddress = this.getRemoteAddress().getAddress();
            remoteInetAddress.getHostName(); // Causes future toString() to print the name too

            LOGGER.debug("SMTP connection from {}, new connection count: {}",
                    remoteInetAddress,
                    this.serverThread.getNumberOfConnections());
        }
    }

    private void handleIOExceptionOnRun(IOException e1) {
        if (!this.quitting) {
            // Send a temporary failure back so that the server will try to resend
            // the message later.
            this.sendResponse("421 4.4.0 Problem attempting to execute commands. Please try again later.");
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Exception during SMTP transaction", e1);
            }
        }
    }

    private void handleExceptionDuringRun(Exception e) {
        LOGGER.error("Unexpected error in the SMTP handler thread", e);
        this.sendResponse("421 4.3.0 Mail system failure, closing transmission channel");
        if (e instanceof RuntimeException re) {
            throw re;
        }
        throw new UnexpectedSMTPServerException("Unexpected exception", e);
    }

    private void onRunCompleted(String originalName) {
        this.closeConnection();
        this.endMessageHandler();
        serverThread.sessionEnded(this);
        Thread.currentThread().setName(originalName);
        MDC.clear();
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
            onCommandLoop();
        }
    }

    private void onCommandLoop() throws IOException {
        try {
            Optional<String> line = readCommandLine();
            if (line.isPresent()) {
                LOGGER.debug("Client: {}", line);
                this.server.getCommandHandler().handleCommand(this, line.get());
            } else {
                LOGGER.debug("no more lines from client");
            }
        } catch (final SocketTimeoutException ex) {
            this.sendResponse("421 Timeout waiting for data from client.");
        } catch (final CRLFTerminatedReader.TerminationException te) {
            final String msg = "501 Syntax error at character position "
                    + te.position()
                    + ". CR and LF must be CRLF paired.  See RFC 2821 #2.7.1.";

            LOGGER.debug(msg);
            this.sendResponse(msg);
        } catch (final CRLFTerminatedReader.MaxLineLengthException mlle) {
            final String msg = "501 " + mlle.getMessage();

            LOGGER.debug(msg);
            this.sendResponse(msg);
        }
    }

    private Optional<String> readCommandLine() throws IOException {
        try {
            return Optional.ofNullable(this.reader.readLine());
        } catch (final SocketException ex) {
            // Lots of clients just "hang up" rather than issuing QUIT,
            // which would
            // fill our logs with the warning in the outer catch.
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error reading client command: " + ex.getMessage(), ex);
            }
            return Optional.empty();
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
            LOGGER.info("Failed to close connection", e);
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
        this.reader = new CRLFTerminatedReader(this.input,StandardCharsets.UTF_8);
        this.writer = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8));

        this.socket.setSoTimeout(CONNECTION_TIMEOUT);
    }

    /**
     * This method is only used by the start tls command
     *
     * @return the current socket to the client
     */
    public Socket getSocket() {
        return this.socket;
    }

    /**
     * Close the client socket if it is open
     */
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

    /**
     * Sends the response to the client
     */
    public void sendResponse(final String response) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Server: {}", response);
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

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) this.socket.getRemoteSocketAddress();
    }

    @Override
    public BaseSmtpServer getSMTPServer() {
        return this.server;
    }

    /**
     * @return the current message handler
     */
    public MessageHandler getMessageHandler() {
        return this.messageHandler;
    }

    /**
     * Simple state
     */
    @Override
    public String getHelo() {
        return this.helo;
    }

    public void setHelo(final String value) {
        this.helo = value;
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
    }

    /**
     * Safely calls done() on a message hander, if one exists
     */
    private void endMessageHandler() {
        if (this.messageHandler != null) {
            try {
                this.messageHandler.done();
            } catch (final Exception ex) {
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
}
