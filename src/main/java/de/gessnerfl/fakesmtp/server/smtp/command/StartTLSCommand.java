package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.Locale;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

/**
 * @author Michael Wildpaner &lt;mike@wildpaner.com&gt;
 * @author Jeff Schnitzer
 */
public class StartTLSCommand extends BaseCommand {
	private final static Logger log = LoggerFactory.getLogger(StartTLSCommand.class);

	public StartTLSCommand() {
		super("STARTTLS", "The starttls command");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		if (!commandString.trim().toUpperCase(Locale.ENGLISH).equals(this.getName())) {
			sess.sendResponse("501 Syntax error (no parameters allowed)");
			return;
		}

		if (!sess.getServer().getEnableTLS()) {
			sess.sendResponse("454 TLS not supported");
			return;
		}

		try {
			final Socket socket = sess.getSocket();
			if (socket instanceof SSLSocket) {
				sess.sendResponse("454 TLS not available due to temporary reason: TLS already active");
				return;
			}

			sess.sendResponse("220 Ready to start TLS");

			try(final SSLSocket s = sess.getServer().createSSLSocket(socket)) {
				s.startHandshake();
				log.debug("Cipher suite: " + s.getSession().getCipherSuite());

				sess.setSocket(s);
				sess.resetSmtpProtocol(); // clean state
				sess.setTlsStarted(true);

				if (s.getNeedClientAuth()) {
					try {
						final Certificate[] peerCertificates = s.getSession().getPeerCertificates();
						sess.setTlsPeerCertificates(peerCertificates);
					} catch (final SSLPeerUnverifiedException e) {
						// IGNORE, just leave the certificate chain null
					}
				}
			}
		} catch (final SSLHandshakeException ex) {
			// "no cipher suites in common" is common and puts a lot of crap in the logs.
			// This will at least limit it to a single WARN line and not a whole stacktrace.
			// Unfortunately it might catch some other types of SSLHandshakeException (if
			// in fact other types exist), but oh well.
			log.warn("startTLS() failed: " + ex);
		} catch (final IOException ex) {
			log.warn("startTLS() failed: " + ex.getMessage(), ex);
		}
	}
}
