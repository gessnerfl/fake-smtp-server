package de.gessnerfl.fakesmtp.smtp;

import java.io.IOException;
import java.io.InputStream;

/**
 * The interface that defines the conversational exchange of a single message on
 * an SMTP connection. Using the term "mail transaction", as defined by RFC
 * 5321, implementing classes of this interface track a single mail transaction.
 * The methods will be called in the following order:
 *
 * <ol>
 * <li><code>from()</code></li>
 * <li><code>recipient()</code> (possibly more than once)</li>
 * <li><code>data()</code></li>
 * <li><code>done()</code></li>
 * </ol>
 *
 * If multiple messages are delivered on a single connection (ie, using the RSET
 * command) then multiple message handlers will be instantiated. Each handler
 * services one and only one message.
 */
public interface MessageHandler {
	/**
	 * Called first, after the MAIL FROM during a SMTP exchange. A MessageHandler is
	 * created after the MAIL command is received, so this function is always
	 * called, even if the mail transaction is aborted later.
	 *
	 * @param from is the sender as specified by the client. It will be a mostly
	 *             rfc822-compliant email address, already validated by the server.
	 *             The validation is performed by the JavaMail InternetAddress.parse
	 *             function, according to the strict rules, which means that "many
	 *             (but not all) of the RFC822 syntax rules are enforced".
	 * @throws RejectException         if the sender should be denied.
	 */
	void from(String from) throws RejectException;

	/**
	 * Called once for every RCPT TO during a SMTP exchange. This will occur after a
	 * from() call.
	 *
	 * @param recipient is a rfc822-compliant email address, validated by the
	 *                  server.
	 * @throws RejectException         if the recipient should be denied.
	 */
	void recipient(String recipient) throws RejectException;

	/**
	 * Called when the DATA part of the SMTP exchange begins. This will occur after
	 * all recipient() calls are complete.
	 *
	 * Note: If you do not read all the data, it will be read for you after this
	 * method completes.
	 *
	 * @param data will be the smtp data stream, stripped of any extra '.' chars.
	 *             The data stream will be valid only for the duration of the call.
	 *
	 * @throws RejectException         if at any point the data should be rejected.
	 * @throws IOException             if there is an IO error reading the input
	 *                                 data.
	 */
	void data(InputStream data) throws RejectException, IOException;

	/**
	 * Called after all other methods are completed. Note that this method will be
	 * called even if the mail transaction is aborted at some point after the
	 * initial from() call.
	 */
	void done();
}
