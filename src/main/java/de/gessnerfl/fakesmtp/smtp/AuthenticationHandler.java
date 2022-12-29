package de.gessnerfl.fakesmtp.smtp;

/**
 * The interface that enables challenge-response communication necessary for
 * SMTP AUTH.
 * <p>
 * Since the authentication process can be stateful, an instance of this class
 * can be stateful too.<br>
 * Do not share a single instance of this interface if you don't explicitly need
 * to do so.
 */
public interface AuthenticationHandler {
	/**
	 * Initially called using an input string in the RFC2554 form: "AUTH
	 * &lt;mechanism&gt; [initial-response]". <br>
	 * This method must return text which will be delivered to the client, or null
	 * if the exchange has been completed successfully. If a response is provided to
	 * the client, this continues the exchange - there will be another auth() call
	 * with the client's response.
	 * <p>
	 * Depending on the authentication mechanism, the handshaking process may
	 * require many request-response passes. This method will return
	 * <code>null</code> only when the authentication process is finished.
	 * <p>
	 * AuthenticationHandlers are associated with a single authentication exchange.
	 * If the exchange is stopped (i.e. fails) and is restarted, a new
	 * AuthenticationHandler is created. Upon successful authentication, your
	 * implementation of this object becomes part of the MessageContext. Your
	 * MessageHandler may upcast your AuthenticationHandler to obtain further
	 * information, such as identity.
	 * <p>
	 * AuthenticationHandlers do not need to handle the "*" cancel response; this is
	 * handled by the framework.
	 *
	 * @return <code>null</code> if the authentication process is finished,
	 *         otherwise a string to hand back to the client.
	 * @param clientInput The client's input, eg "AUTH PLAIN dGVzdAB0ZXN0ADEyMzQ="
	 * @throws RejectException if authentication fails.
	 */
	String auth(String clientInput) throws RejectException;

	/**
	 * If the authentication process was successful, this returns the identity of
	 * the user. The type defining the identity can vary depending on the
	 * authentication mechanism used, but typically this returns a String username.
	 * If authentication was not successful, the return value is undefined.
	 */
	Object getIdentity();
}
