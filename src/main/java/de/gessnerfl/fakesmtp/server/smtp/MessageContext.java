package de.gessnerfl.fakesmtp.server.smtp;

import java.net.SocketAddress;
import java.security.cert.Certificate;

import de.gessnerfl.fakesmtp.server.smtp.server.SMTPServer;

/**
 * Interface which provides context to the message handlers.
 */
public interface MessageContext {
	/**
	 * @return the SMTPServer object.
	 */
	SMTPServer getSMTPServer();

	/**
	 * @return the IP address of the remote server.
	 */
	SocketAddress getRemoteAddress();

	/**
	 * @return the handler instance that was used to authenticate.
	 */
	AuthenticationHandler getAuthenticationHandler();

	/**
	 * @return the host name or address literal the client supplied in the HELO or
	 *         EHLO command, or null if neither of these commands were received yet.
	 *         Note that SubEthaSMTP (along with some MTAs, but contrary to RFC
	 *         5321) accept mail transactions without these commands.
	 */
	String getHelo();

	/**
	 * Returns the identity of the peer which was established as part of the TLS
	 * handshake as defined by
	 * {@link javax.net.ssl.SSLSession#getPeerCertificates()}.
	 *
	 * <p>
	 * In order to get this information, override
	 * {@link SMTPServer#createSSLSocket(java.net.Socket)} and call
	 * {@link javax.net.ssl.SSLSocket#setNeedClientAuth(boolean)
	 * setNeedClientAuth(true)} on the created socket.
	 *
	 * @return an ordered array of peer certificates, with the peer's own
	 *         certificate first followed by any certificate authorities, or null
	 *         when no such information is available
	 * @see javax.net.ssl.SSLSession#getPeerCertificates()
	 */
	Certificate[] getTlsPeerCertificates();
}
