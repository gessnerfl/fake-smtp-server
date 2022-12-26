package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;
import java.util.Locale;

import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandler;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.RejectException;
import de.gessnerfl.fakesmtp.server.smtp.io.CRLFTerminatedReader;
import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

public class AuthCommand extends BaseCommand {
	public static final String VERB = "AUTH";

	public static final String AUTH_CANCEL_COMMAND = "*";

	/** Creates a new instance of AuthCommand */
	public AuthCommand() {
		super(VERB,
				"Authentication service",
				VERB
						+ " <mechanism> [initial-response] \n"
						+ "\t mechanism = a string identifying a SASL authentication mechanism,\n"
						+ "\t an optional base64-encoded response");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		if (sess.isAuthenticated()) {
			sess.sendResponse("503 Refusing any other AUTH command.");
			return;
		}

		final AuthenticationHandlerFactory authFactory = sess.getServer().getAuthenticationHandlerFactory();

		if (authFactory == null) {
			sess.sendResponse("502 Authentication not supported");
			return;
		}

		final AuthenticationHandler authHandler = authFactory.create();

		final String[] args = this.getArgs(commandString);
		// Let's check the command syntax
		if (args.length < 2) {
			sess.sendResponse("501 Syntax: " + VERB + " mechanism [initial-response]");
			return;
		}

		// Let's check if we support the required authentication mechanism
		final String mechanism = args[1];
		if (!authFactory.getAuthenticationMechanisms().contains(mechanism.toUpperCase(Locale.ENGLISH))) {
			sess.sendResponse("504 The requested authentication mechanism is not supported");
			return;
		}
		// OK, let's go trough the authentication process.
		try {
			// The authentication process may require a series of challenge-responses
			final CRLFTerminatedReader reader = sess.getReader();

			String response = authHandler.auth(commandString);
			if (response != null) {
				// challenge-response iteration
				sess.sendResponse(response);
			}

			while (response != null) {
				final String clientInput = reader.readLine();
				if (clientInput.trim().equals(AUTH_CANCEL_COMMAND)) {
					// RFC 2554 explicitly states this:
					sess.sendResponse("501 Authentication canceled by client.");
					return;
				}
				response = authHandler.auth(clientInput);
				if (response != null) {
					// challenge-response iteration
					sess.sendResponse(response);
				}
			}

			sess.sendResponse("235 Authentication successful.");
			sess.setAuthenticationHandler(authHandler);
		} catch (final RejectException authFailed) {
			sess.sendResponse(authFailed.getErrorResponse());
		}
	}
}
