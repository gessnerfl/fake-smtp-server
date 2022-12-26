package de.gessnerfl.fakesmtp.server.smtp.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandler;
import de.gessnerfl.fakesmtp.server.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.server.smtp.RejectException;

/**
 * This handler combines the behavior of several other authentication handler
 * factories.
 */
public class MultipleAuthenticationHandlerFactory implements AuthenticationHandlerFactory {
	/**
	 * Maps the auth type (eg "PLAIN") to a handler. The mechanism name (key) is in
	 * upper case.
	 */
	Map<String, AuthenticationHandlerFactory> plugins = new HashMap<>();

	/**
	 * A more orderly list of the supported mechanisms. Mechanism names are in upper
	 * case.
	 */
	List<String> mechanisms = new ArrayList<>();

	public MultipleAuthenticationHandlerFactory() {
		// Starting with an empty list is ok, let the user add them all
	}

	public MultipleAuthenticationHandlerFactory(final Collection<AuthenticationHandlerFactory> factories) {
		for (final AuthenticationHandlerFactory fact : factories) {
			this.addFactory(fact);
		}
	}

	public void addFactory(final AuthenticationHandlerFactory fact) {
		final List<String> partialMechanisms = fact.getAuthenticationMechanisms();
		for (final String mechanism : partialMechanisms) {
			if (!this.mechanisms.contains(mechanism)) {
				this.mechanisms.add(mechanism);
				this.plugins.put(mechanism, fact);
			}
		}
	}

	@Override
	public List<String> getAuthenticationMechanisms() {
		return this.mechanisms;
	}

	@Override
	public AuthenticationHandler create() {
		return new Handler();
	}

	/**
	 */
	class Handler implements AuthenticationHandler {
		AuthenticationHandler active;

		/* */
		@Override
		public String auth(final String clientInput) throws RejectException {
			if (this.active == null) {
				final StringTokenizer stk = new StringTokenizer(clientInput);
				final String auth = stk.nextToken();
				if (!"AUTH".equalsIgnoreCase(auth)) {
					throw new IllegalArgumentException("Not an AUTH command: " + clientInput);
				}

				final String method = stk.nextToken();
				final AuthenticationHandlerFactory fact
						= MultipleAuthenticationHandlerFactory.this.plugins.get(method.toUpperCase(Locale.ENGLISH));

				if (fact == null) {
					throw new RejectException(504, "Method not supported");
				}

				this.active = fact.create();
			}

			return this.active.auth(clientInput);
		}

		/* */
		@Override
		public Object getIdentity() {
			return this.active.getIdentity();
		}
	}
}
