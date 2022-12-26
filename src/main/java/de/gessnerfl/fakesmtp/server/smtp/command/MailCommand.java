package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.IOException;
import java.util.Locale;

import de.gessnerfl.fakesmtp.server.smtp.DropConnectionException;
import de.gessnerfl.fakesmtp.server.smtp.RejectException;
import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;
import de.gessnerfl.fakesmtp.server.smtp.util.EmailUtils;

public class MailCommand extends BaseCommand {
	public MailCommand() {
		super("MAIL", "Specifies the sender.", "FROM: <sender> [ <parameters> ]");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.subethamail.smtp.server.BaseCommand#execute(java.lang.String,
	 * org.subethamail.smtp.server.Session)
	 */
	@Override
	public void execute(final String commandString, final Session sess) throws IOException, DropConnectionException {
		if (sess.isMailTransactionInProgress()) {
			sess.sendResponse("503 5.5.1 Sender already specified.");
			return;
		}

		if (commandString.trim().equals("MAIL FROM:")) {
			sess.sendResponse("501 Syntax: MAIL FROM: <address>");
			return;
		}

		final String args = this.getArgPredicate(commandString);
		if (!args.toUpperCase(Locale.ENGLISH).startsWith("FROM:")) {
			sess.sendResponse("501 Syntax: MAIL FROM: <address>  Error in parameters: \""
					+ this.getArgPredicate(commandString)
					+ "\"");
			return;
		}

		final String emailAddress = EmailUtils.extractEmailAddress(args, 5);
		if (!EmailUtils.isValidEmailAddress(emailAddress)) {
			sess.sendResponse("553 <" + emailAddress + "> Invalid email address.");
			return;
		}

		// extract SIZE argument from MAIL FROM command.
		// disregard unknown parameters. TODO: reject unknown
		// parameters.
		int size = 0;
		final String largs = args.toLowerCase(Locale.ENGLISH);
		final int sizec = largs.indexOf(" size=");
		if (sizec > -1) {
			// disregard non-numeric values.
			final String ssize = largs.substring(sizec + 6).trim();
			if (ssize.length() > 0 && ssize.matches("[0-9]+")) {
				size = Integer.parseInt(ssize);
			}
		}
		// Reject the message if the size supplied by the client
		// is larger than what we advertised in EHLO answer.
		if (size > sess.getServer().getMaxMessageSize()) {
			sess.sendResponse("552 5.3.4 Message size exceeds fixed limit");
			return;
		}

		sess.setDeclaredMessageSize(size);
		sess.startMailTransaction();

		try {
			sess.getMessageHandler().from(emailAddress);
		} catch (final DropConnectionException ex) {
			// roll back the start of the transaction
			sess.resetMailTransaction();
			throw ex; // Propagate this
		} catch (final RejectException ex) {
			// roll back the start of the transaction
			sess.resetMailTransaction();
			sess.sendResponse(ex.getErrorResponse());
			return;
		}

		sess.sendResponse("250 Ok");
	}
}
