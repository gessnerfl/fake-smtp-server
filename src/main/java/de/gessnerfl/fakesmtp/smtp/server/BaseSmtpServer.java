package de.gessnerfl.fakesmtp.smtp.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

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
 * pass in a SimpleMessageListenerAdapter.
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
    private static final int BACKLOG = 50;

    private InetAddress bindAddress = null; // default to all interfaces
    private int port = 25; // default to 25
    private String hostName; // defaults to a lookup of the local address
    private final String softwareName;
    private final MessageHandlerFactory messageHandlerFactory;
    private AuthenticationHandlerFactory authenticationHandlerFactory;
    private final CommandHandler commandHandler;
    private Thread serverThread;
    private BaseSmtpServerRunnable baseSmtpServerRunnable;
    private final boolean virtualThreadsEnabled;

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
     * If true, a TLS handshake is required; ignored if enableTLS=false
     */
    private boolean requireTLS = false;

    /**
     * If true, this server will accept no mail until auth succeeded; ignored if no
     * AuthenticationHandlerFactory has been set
     */
    private boolean requireAuth = false;

    /**
     * The maximum size of a message that the server will accept. This value is
     * advertised during the EHLO phase if it is larger than 0. If the message size
     * specified by the client during the MAIL phase, the message will be rejected
     * at that time. (RFC 1870) Default is 0. Note this doesn't actually enforce any
     * limits on the message being read; you must do that yourself when reading
     * data.
     */
    private long maxMessageSizeInBytes = 0;

    private final SessionIdFactory sessionIdFactory;

    /**
     * Simple constructor.
     */
    public BaseSmtpServer(final String softwareName,
                          final MessageHandlerFactory handlerFactory,
                          final CommandHandler commandHandler,
                          final SessionIdFactory sessionIdFactory,
                          final boolean virtualThreadsEnabled) {
        this(softwareName, handlerFactory, commandHandler, sessionIdFactory, null, virtualThreadsEnabled);
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
                          final CommandHandler commandHandler,
                          final SessionIdFactory sessionIdFactory,
                          final AuthenticationHandlerFactory authHandlerFact,
                          final boolean virtualThreadsEnabled) {
        this.softwareName = softwareName;
        this.messageHandlerFactory = msgHandlerFact;
        this.authenticationHandlerFactory = authHandlerFact;
        this.commandHandler = commandHandler;
        this.sessionIdFactory = sessionIdFactory;
        this.virtualThreadsEnabled = virtualThreadsEnabled;

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

    public boolean isVirtualThreadsEnabled() {
        return virtualThreadsEnabled;
    }

    /**
     * null means all interfaces
     */
    public void setBindAddress(final InetAddress bindAddress) {
        this.bindAddress = bindAddress;
    }

    @Override
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

        final var serverThread = new BaseSmtpServerRunnable(this, serverSocket);
        final var threadBuilder = isVirtualThreadsEnabled() ? Thread.ofVirtual() : Thread.ofPlatform();
        this.serverThread = threadBuilder.name(BaseSmtpServerRunnable.class.getName() + " " + getDisplayableLocalSocketAddress()).start(serverThread);
        this.baseSmtpServerRunnable = serverThread;

        this.started = true;
    }

    /**
     * Shut things down gracefully.
     */
    @Override
    @PreDestroy
    public synchronized void stop() {
        LOGGER.info("SMTP server {} stopping...", getDisplayableLocalSocketAddress());
        if (this.baseSmtpServerRunnable != null) {
            this.baseSmtpServerRunnable.shutdown();
            this.baseSmtpServerRunnable = null;
        }
        if(this.serverThread != null) {
            this.serverThread.interrupt();
            try {
                this.serverThread.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("SMTP server {} stopped", getDisplayableLocalSocketAddress());
    }

    @SuppressWarnings("java:S2095")
    private ServerSocket createServerSocket() throws IOException {
        InetSocketAddress isa;

        if (this.bindAddress == null) {
            isa = new InetSocketAddress(this.port);
        } else {
            isa = new InetSocketAddress(this.bindAddress, this.port);
        }

        final ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(isa, BACKLOG);

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
    public long getMaxMessageSizeInBytes() {
        return maxMessageSizeInBytes;
    }

    /**
     * @param maxMessageSizeInBytes the maxMessageSize to set
     */
    public void setMaxMessageSizeInBytes(final long maxMessageSizeInBytes) {
        this.maxMessageSizeInBytes = maxMessageSizeInBytes;
    }

    public SessionIdFactory getSessionIdFactory() {
        return sessionIdFactory;
    }
}
