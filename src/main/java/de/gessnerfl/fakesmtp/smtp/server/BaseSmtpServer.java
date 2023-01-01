package de.gessnerfl.fakesmtp.smtp.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.gessnerfl.fakesmtp.smtp.command.CommandHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.gessnerfl.fakesmtp.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.smtp.MessageHandlerFactory;

/**
 * Main SMTPServer class. Construct this object, set the hostName, port, and
 * bind address if you wish to override the defaults, and call start().
 * <p>
 * This class starts opens a ServerSocket and creates a new instance of the
 * ConnectionHandler class when a new connection comes in. The ConnectionHandler
 * then parses the incoming SMTP stream and hands off the processing to the
 * CommandHandler which will execute the appropriate SMTP command class.
 * <p>
 * To use this class, construct a server with your implementation of the
 * MessageHandlerFactory. This provides low-level callbacks at various phases of
 * the SMTP exchange. For a higher-level but more limited interface, you can
 * pass in a org.subethamail.smtp.helper.SimpleMessageListenerAdapter.
 * <p>
 * By default, no authentication methods are offered. To use authentication, set
 * an AuthenticationHandlerFactory.
 */
public class BaseSmtpServer implements SmtpServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSmtpServer.class);

    /**
     * Hostname used if we can't find one
     */
    private static final String UNKNOWN_HOSTNAME = "localhost";

    private InetAddress bindAddress = null; // default to all interfaces

    private int port = 25; // default to 25

    private String hostName; // defaults to a lookup of the local address

    private int backlog = 50;

    private final String softwareName;

    private MessageHandlerFactory messageHandlerFactory;

    private AuthenticationHandlerFactory authenticationHandlerFactory;

    private final CommandHandler commandHandler;

    /**
     * The thread listening on the server socket.
     */
    private ServerThread serverThread;

    private boolean updateThreadName = true;

    /**
     * True if this SMTPServer was started. It remains true even if the SMTPServer
     * has been stopped since. It is used to prevent restarting this object. Even if
     * it was shutdown properly, it cannot be restarted, because the contained
     * thread pool object itself cannot be restarted.
     **/
    private boolean started = false;

    /**
     * If true, TLS is enabled
     */
    private boolean enableTLS = false;

    /**
     * If true, TLS is not announced; ignored if enableTLS=false
     */
    private boolean hideTLS = false;

    /**
     * If true, a TLS handshake is required; ignored if enableTLS=false
     */
    private boolean requireTLS = false;

    /**
     * If true, this server will accept no mail until auth succeeded; ignored if no
     * AuthenticationHandlerFactory has been set
     */
    private boolean requireAuth = false;

    /**
     * If true, no Received headers will be inserted
     */
    private boolean disableReceivedHeaders = false;

    /**
     * set a hard limit on the maximum number of connections this server will accept
     * once we reach this limit, the server will gracefully reject new connections.
     * Default is 1000.
     */
    private int maxConnections = 1000;

    /**
     * The timeout for waiting for data on a connection is one minute: 1000 * 60 * 1
     */
    private int connectionTimeout = 1000 * 60 * 1;

    /**
     * The maximal number of recipients that this server accepts per message
     * delivery request.
     */
    private int maxRecipients = 1000;

    /**
     * The maximum size of a message that the server will accept. This value is
     * advertised during the EHLO phase if it is larger than 0. If the message size
     * specified by the client during the MAIL phase, the message will be rejected
     * at that time. (RFC 1870) Default is 0. Note this doesn't actually enforce any
     * limits on the message being read; you must do that yourself when reading
     * data.
     */
    private int maxMessageSize = 0;

    private SessionIdFactory sessionIdFactory = new TimeBasedSessionIdFactory();

    /**
     * Simple constructor.
     */
    public BaseSmtpServer(final String softwareName, final MessageHandlerFactory handlerFactory) {
        this(softwareName, handlerFactory, null);
    }

    /**
     * @param authHandlerFact the {@link AuthenticationHandlerFactory} which
     *                        performs authentication in the SMTP AUTH command. If
     *                        null, authentication is not supported. Note that
     *                        setting an authentication handler does not enforce
     *                        authentication, it only makes authentication possible.
     *                        Enforcing authentication is the responsibility of the
     *                        client application, which usually enforces it only
     *                        selectively. Use {@link Session#isAuthenticated} to
     *                        check whether the client was authenticated in the
     *                        session.
     */
    public BaseSmtpServer(final String softwareName,
                          final MessageHandlerFactory msgHandlerFact,
                          final AuthenticationHandlerFactory authHandlerFact) {
        this.softwareName = softwareName;
        this.messageHandlerFactory = msgHandlerFact;
        this.authenticationHandlerFactory = authHandlerFact;
        this.commandHandler = new CommandHandler();

        try {
            this.hostName = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (final UnknownHostException e) {
            this.hostName = UNKNOWN_HOSTNAME;
        }
    }

    /**
     * @return the host name that will be reported to SMTP clients
     */
    public String getHostName() {
        return this.hostName == null ? UNKNOWN_HOSTNAME : this.hostName;
    }

    /**
     * The host name that will be reported to SMTP clients
     */
    public void setHostName(final String hostName) {
        this.hostName = hostName;
    }

    /**
     * null means all interfaces
     */
    public InetAddress getBindAddress() {
        return this.bindAddress;
    }

    /**
     * null means all interfaces
     */
    public void setBindAddress(final InetAddress bindAddress) {
        this.bindAddress = bindAddress;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * The string reported to the public as the software running here. Defaults to
     * SubEthaSTP and the version number.
     */
    public String getSoftwareName() {
        return this.softwareName;
    }

    /**
     * Is the server running after start() has been called?
     */
    public synchronized boolean isRunning() {
        return this.serverThread != null;
    }

    /**
     * The backlog is the Socket backlog.
     * <p>
     * The backlog argument must be a positive value greater than 0. If the value
     * passed if equal or less than 0, then the default value will be assumed.
     *
     * @return the backlog
     */
    public int getBacklog() {
        return this.backlog;
    }

    /**
     * The backlog is the Socket backlog.
     * <p>
     * The backlog argument must be a positive value greater than 0. If the value
     * passed if equal or less than 0, then the default value will be assumed.
     */
    public void setBacklog(final int backlog) {
        this.backlog = backlog;
    }

    /**
     * Call this method to get things rolling after instantiating the SMTPServer.
     * <p>
     * An SMTPServer which has been shut down, must not be reused.
     */
    @Override
    @PostConstruct
    public synchronized void start() {
        LOGGER.info("SMTP server {} starting", getDisplayableLocalSocketAddress());
        if (this.started) {
            throw new IllegalStateException("SMTPServer can only be started once. Restarting is not allowed even after a proper shutdown.");
        }

        // Create our server socket here.
        ServerSocket serverSocket;
        try {
            serverSocket = this.createServerSocket();
        } catch (final Exception e) {
            throw new FailedToCreateServerSocketException(e);
        }

        this.serverThread = new ServerThread(this, serverSocket);
        this.serverThread.setUpdateThreadName(isUpdateThreadName());
        this.serverThread.start();
        this.started = true;
    }

    /**
     * Shut things down gracefully.
     */
    @Override
    @PreDestroy
    public synchronized void stop() {
        LOGGER.info("SMTP server {} stopping...", getDisplayableLocalSocketAddress());
        if (this.serverThread != null) {
            this.serverThread.shutdown();
            this.serverThread = null;
            LOGGER.info("SMTP server {} stopped", getDisplayableLocalSocketAddress());
        }
    }

    private ServerSocket createServerSocket() throws IOException {
        InetSocketAddress isa;

        if (this.bindAddress == null) {
            isa = new InetSocketAddress(this.port);
        } else {
            isa = new InetSocketAddress(this.bindAddress, this.port);
        }

        final ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(isa, this.backlog);

        if (this.port == 0) {
            this.port = serverSocket.getLocalPort();
        }

        return serverSocket;
    }

    /**
     * Create a SSL socket that wraps the existing socket. This method is called
     * after the client issued the STARTTLS command.
     * <p>
     * Subclasses may override this method to configure the key stores, enabled
     * protocols/ cipher suites, enforce client authentication, etc.
     *
     * @param socket the existing server socket
     * @return a SSLSocket
     * @throws IOException when creating the socket failed
     */
    public SSLSocket createSSLSocket(final Socket socket) throws IOException {
        final SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        final InetSocketAddress remoteAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        final SSLSocket s = (SSLSocket) sf.createSocket(socket, remoteAddress.getHostName(), socket.getPort(), true);
        s.setUseClientMode(false);
        s.setEnabledCipherSuites(s.getSupportedCipherSuites());
        return s;
    }

    public String getDisplayableLocalSocketAddress() {
        return (this.bindAddress == null ? "*" : this.bindAddress) + ":" + this.port;
    }

    /**
     * @return the factory for message handlers, cannot be null
     */
    public MessageHandlerFactory getMessageHandlerFactory() {
        return this.messageHandlerFactory;
    }

    public void setMessageHandlerFactory(final MessageHandlerFactory fact) {
        this.messageHandlerFactory = fact;
    }

    /**
     * @return the factory for auth handlers, or null if no such factory has been
     * set.
     */
    public AuthenticationHandlerFactory getAuthenticationHandlerFactory() {
        return this.authenticationHandlerFactory;
    }

    public void setAuthenticationHandlerFactory(final AuthenticationHandlerFactory fact) {
        this.authenticationHandlerFactory = fact;
    }

    /**
     * The CommandHandler manages handling the SMTP commands such as QUIT, MAIL,
     * RCPT, DATA, etc.
     *
     * @return An instance of CommandHandler
     */
    public CommandHandler getCommandHandler() {
        return this.commandHandler;
    }

    public int getMaxConnections() {
        return this.maxConnections;
    }

    /**
     * Set's the maximum number of connections this server instance will accept.
     *
     * @param maxConnections the maximum number of connections to accept
     */
    public void setMaxConnections(final int maxConnections) {
        if (this.isRunning()) {
            throw new ServerAlreadyRunningException("Server is already running. It isn't possible to set the maxConnections. Please stop the server first.");
        }

        this.maxConnections = maxConnections;
    }

    public int getConnectionTimeout() {
        return this.connectionTimeout;
    }

    /**
     * Set the number of milliseconds that the server will wait for client input.
     * Sometime after this period expires, an client will be rejected and the
     * connection closed.
     */
    public void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getMaxRecipients() {
        return this.maxRecipients;
    }

    /**
     * Set the maximum number of recipients allowed for each message. A value of -1
     * means "unlimited".
     */
    public void setMaxRecipients(final int maxRecipients) {
        this.maxRecipients = maxRecipients;
    }

    /**
     * If set to true, TLS will be supported.
     * <p>
     * The minimal JSSE configuration necessary for a working TLS support on Oracle
     * JRE 6:
     * <ul>
     * <li>javax.net.ssl.keyStore system property must refer to a file containing a
     * JKS keystore with the private key.
     * <li>javax.net.ssl.keyStorePassword system property must specify the keystore
     * password.
     * </ul>
     * <p>
     */
    public void setEnableTLS(final boolean enableTLS) {
        this.enableTLS = enableTLS;
    }

    public boolean getEnableTLS() {
        return enableTLS;
    }

    public boolean getHideTLS() {
        return this.hideTLS;
    }

    /**
     * If set to true, TLS will not be advertised in the EHLO string. Default is
     * false; true implied when disableTLS=true.
     */
    public void setHideTLS(final boolean value) {
        this.hideTLS = value;
    }

    public boolean getRequireTLS() {
        return this.requireTLS;
    }

    /**
     * @param requireTLS true to require a TLS handshake, false to allow operation
     *                   with or without TLS. Default is false; ignored when
     *                   disableTLS=true.
     */
    public void setRequireTLS(final boolean requireTLS) {
        this.requireTLS = requireTLS;
    }

    public boolean getRequireAuth() {
        return requireAuth;
    }

    /**
     * @param requireAuth true for mandatory smtp authentication, i.e. no mail mail
     *                    be accepted until authentication succeeds. Don't forget to
     *                    set AuthenticationHandlerFactory to allow client
     *                    authentication. Defaults to false.
     */
    public void setRequireAuth(final boolean requireAuth) {
        this.requireAuth = requireAuth;
    }

    /**
     * @return the maxMessageSize
     */
    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    /**
     * @param maxMessageSize the maxMessageSize to set
     */
    public void setMaxMessageSize(final int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public boolean getDisableReceivedHeaders() {
        return disableReceivedHeaders;
    }

    /**
     * @param disableReceivedHeaders false to include Received headers. Default is
     *                               false.
     */
    public void setDisableReceivedHeaders(final boolean disableReceivedHeaders) {
        this.disableReceivedHeaders = disableReceivedHeaders;
    }

    public SessionIdFactory getSessionIdFactory() {
        return sessionIdFactory;
    }

    /**
     * Sets the {@link SessionIdFactory} which will allocate a unique identifier for
     * each mail sessions. If not set, a reasonable default will be used.
     */
    public void setSessionIdFactory(final SessionIdFactory sessionIdFactory) {
        this.sessionIdFactory = sessionIdFactory;
    }

    public boolean isUpdateThreadName() {
        return updateThreadName;
    }

    public void setUpdateThreadName(final boolean updateThreadName) {
        this.updateThreadName = updateThreadName;
    }
}
