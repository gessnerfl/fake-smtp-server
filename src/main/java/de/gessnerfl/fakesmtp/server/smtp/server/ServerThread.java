package de.gessnerfl.fakesmtp.server.smtp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * ServerThread accepts TCP connections to the server socket and starts a new
 * {@link Session} thread for each connection which will handle the connection.
 * On shutdown it terminates not only this thread, but the session threads too.
 */
class ServerThread extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerThread.class);

	private final SMTPServer server;

	private final ServerSocket serverSocket;

	/**
	 * A semaphore which is used to prevent accepting new connections by blocking
	 * this thread if the allowed count of open connections is already reached.
	 */
	private final Semaphore connectionPermits;

	/**
	 * The list of currently running sessions.
	 */
	private final Set<Session> sessionThreads;

	/**
	 * A flag which indicates that this SMTP port and all of its open connections
	 * are being shut down.
	 */
	private volatile boolean shuttingDown;

	private boolean updateThreadName = true;

	public ServerThread(final SMTPServer server, final ServerSocket serverSocket) {
		super(ServerThread.class.getName() + " " + server.getDisplayableLocalSocketAddress());
		this.server = server;
		this.serverSocket = serverSocket;
		// reserve a few places for graceful disconnects with informative
		// messages
		final int countOfConnectionPermits = server.getMaxConnections() + 10;
		this.connectionPermits = new Semaphore(countOfConnectionPermits);
		this.sessionThreads = new HashSet<>(countOfConnectionPermits * 4 / 3 + 1);
	}

	/**
	 * This method is called by this thread when it starts up. To safely cause this
	 * to exit, call {@link #shutdown()}.
	 */
	@Override
	public void run() {
		MDC.put("smtpServerLocalSocketAddress", server.getDisplayableLocalSocketAddress());
		LOGGER.info("SMTP server {} started", server.getDisplayableLocalSocketAddress());

		try {
			runAcceptLoop();
			LOGGER.info("SMTP server {} stopped accepting connections", server.getDisplayableLocalSocketAddress());
		} finally {
			MDC.remove("smtpServerLocalSocketAddress");
		}
	}

	/**
	 * Accept connections and run them in session threads until shutdown.
	 */
	private void runAcceptLoop() {
		while (!this.shuttingDown) {
			onServerLoop();
		}
	}

	private void onServerLoop() {
		acquireConnectionPermit();

		acceptServerSocket(this.serverSocket)
				.flatMap(this::establishSocketSession)
				.map(this::addSessionsSynchronizedToActiveList)
				.ifPresent(this::executeSocketSession);
	}

	private void acquireConnectionPermit() {
		try {
			// block if too many connections are open
			connectionPermits.acquire();
		} catch (final InterruptedException consumed) {
			Thread.currentThread().interrupt();
		}
	}

	private Optional<Socket> acceptServerSocket(ServerSocket serverSocket){
		try {
			return Optional.of(serverSocket.accept());
		} catch (final IOException e) {
			connectionPermits.release();
			// it also happens during shutdown, when the socket is closed
			if (!this.shuttingDown) {
				LOGGER.error("Error accepting connection", e);
				// prevent a possible loop causing 100% processor usage
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException consumed) {
					Thread.currentThread().interrupt();
				}
			}
			return Optional.empty();
		}
	}

	private Optional<SocketSession> establishSocketSession(Socket socket){
		try {
			var session = new Session(server, this, socket);
			session.setUpdateThreadName(isUpdateThreadName());
			return Optional.of(new SocketSession(socket, session));
		} catch (final IOException e) {
			connectionPermits.release();
			LOGGER.error("Error while starting a connection", e);
			try {
				socket.close();
			} catch (final IOException e1) {
				LOGGER.debug("Cannot close socket after exception", e1);
			}
			return Optional.empty();
		}
	}

	private SocketSession addSessionsSynchronizedToActiveList(SocketSession socketSession) {
		// add thread before starting it,
		// because it will check the count of sessions
		synchronized (this) {
			this.sessionThreads.add(socketSession.session);
		}
		return socketSession;
	}

	private void executeSocketSession(SocketSession socketSession){
		try {
			server.getExecutorService().execute(socketSession.session);
		} catch (final RejectedExecutionException e) {
			connectionPermits.release();
			synchronized (this) {
				this.sessionThreads.remove(socketSession.session);
			}
			LOGGER.error("Error while executing a session", e);
			try {
				socketSession.socket.close();
			} catch (final IOException e1) {
				LOGGER.debug("Cannot close socket after exception", e1);
			}
		}
	}

	/**
	 * Closes the server socket and all client sockets.
	 */
	public void shutdown() {
		// First make sure we aren't accepting any new connections
		shutdownServerThread();
		// Shut down any open connections.
		shutdownSessions();
	}

	private void shutdownServerThread() {
		shuttingDown = true;
		closeServerSocket();
		interrupt();
		try {
			join();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Closes the serverSocket in an orderly way.
	 */
	private void closeServerSocket() {
		try {
			this.serverSocket.close();
			LOGGER.debug("SMTP Server socket shut down");
		} catch (final IOException e) {
			LOGGER.error("Failed to close server socket.", e);
		}
	}

	private void shutdownSessions() {
		// Copy the sessionThreads collection so the guarding lock on this
		// instance can be released before calling the Session.shutdown methods.
		// This is necessary to avoid a deadlock, because the terminating
		// session threads call back the sessionEnded function in this instance,
		// which locks this instance.
		List<Session> sessionsToBeClosed;
		synchronized (this) {
			sessionsToBeClosed = new ArrayList<>(sessionThreads);
		}
		for (final Session sessionThread : sessionsToBeClosed) {
			sessionThread.quit();
		}

		server.getExecutorService().shutdown();
		try {
			server.getExecutorService().awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (final InterruptedException e) {
			LOGGER.warn("Interrupted waiting for termination of session threads", e);
			Thread.currentThread().interrupt();
		}
	}

	public synchronized boolean hasTooManyConnections() {
		return sessionThreads.size() > server.getMaxConnections();
	}

	public synchronized int getNumberOfConnections() {
		return sessionThreads.size();
	}

	/**
	 * Registers that the specified {@link Session} thread ended. Session threads
	 * must call this function.
	 */
	public void sessionEnded(final Session session) {
		synchronized (this) {
			sessionThreads.remove(session);
		}
		connectionPermits.release();
	}

	public boolean isUpdateThreadName() {
		return updateThreadName;
	}

	public void setUpdateThreadName(final boolean updateThreadName) {
		this.updateThreadName = updateThreadName;
	}

	private static final class SocketSession {
		final Socket socket;
		final Session session;

		public SocketSession(Socket socket, Session session) {
			this.socket = socket;
			this.session = session;
		}
	}
}
