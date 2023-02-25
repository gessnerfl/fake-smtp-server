package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;
import java.util.Locale;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.gessnerfl.fakesmtp.smtp.server.Session;

public class StartTLSCommand extends BaseCommand {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartTLSCommand.class);

	public StartTLSCommand() {
		super(CommandVerb.STARTTLS, "The starttls command");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		if (!commandString.trim().toUpperCase(Locale.ENGLISH).equals(this.getVerb().name())) {
			sess.sendResponse("501 Syntax error (no parameters allowed)");
			return;
		}

		if (!sess.getServer().getEnableTLS()) {
			sess.sendResponse("454 TLS not supported");
			return;
		}

		try {
			final var socket = sess.getSocket();
			if (socket instanceof SSLSocket) {
				sess.sendResponse("454 TLS not available due to temporary reason: TLS already active");
				return;
			}

			sess.sendResponse("220 Ready to start TLS");

			try(final SSLSocket s = sess.getServer().createSSLSocket(socket)) {
				s.startHandshake();
				LOGGER.debug("Cipher suite: {}", s.getSession().getCipherSuite());

				sess.setSocket(s);
				sess.resetSmtpProtocol(); // clean state
				sess.setTlsStarted(true);

				if (s.getNeedClientAuth()) {
					setTlsCertificates(sess, s);
				}
			}
		} catch (final SSLHandshakeException ex) {
			// "no cipher suites in common" is common and puts a lot of crap in the logs.
			// This will at least limit it to a single WARN line and not a whole stacktrace.
			// Unfortunately it might catch some other types of SSLHandshakeException (if
			// in fact other types exist), but oh well.
			LOGGER.warn("startTLS() failed:", ex);
		} catch (final IOException ex) {
			LOGGER.warn("startTLS() failed: {}", ex.getMessage(), ex);
		}
	}

	private static void setTlsCertificates(Session sess, SSLSocket s) {
		try {
			final var peerCertificates = s.getSession().getPeerCertificates();
			sess.setTlsPeerCertificates(peerCertificates);
		} catch (final SSLPeerUnverifiedException e) {
			// IGNORE, just leave the certificate chain null
		}
	}
}
