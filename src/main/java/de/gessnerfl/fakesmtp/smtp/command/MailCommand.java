package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;
import java.util.Locale;

import de.gessnerfl.fakesmtp.smtp.RejectException;
import de.gessnerfl.fakesmtp.smtp.server.Session;
import de.gessnerfl.fakesmtp.smtp.util.EmailUtils;

public class MailCommand extends BaseCommand {
	public MailCommand() {
		super(CommandVerb.MAIL, "Specifies the sender.", "FROM: <sender> [ <parameters> ]");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		if (sess.isMailTransactionInProgress()) {
			sess.sendResponse("503 5.5.1 Sender already specified.");
			return;
		}

		if (commandString.trim().equals("MAIL FROM:")) {
			sess.sendResponse("501 Syntax: MAIL FROM: <address>");
			return;
		}

		final var args = this.getArgPredicate(commandString);
		if (!args.toUpperCase(Locale.ENGLISH).startsWith("FROM:")) {
			sess.sendResponse("501 Syntax: MAIL FROM: <address>  Error in parameters: \""
					+ this.getArgPredicate(commandString)
					+ "\"");
			return;
		}

		final var emailAddress = EmailUtils.extractEmailAddress(args, 5);
		if (!EmailUtils.isValidEmailAddress(emailAddress)) {
			sess.sendResponse("553 <" + emailAddress + "> Invalid email address.");
			return;
		}

		// extract SIZE argument from MAIL FROM command.
		// disregard unknown parameters
		long size = 0;
		final var largs = args.toLowerCase(Locale.ENGLISH);
		final var sizec = largs.indexOf(" size=");
		if (sizec > -1) {
			// disregard non-numeric values.
			final var ssize = largs.substring(sizec + 6).trim();
			if (ssize.length() > 0 && ssize.matches("\\d+")) {
				size = Long.parseLong(ssize);
			}
		}
		// Reject the message if the size supplied by the client
		// is larger than what we advertised in EHLO answer.
		if (size > sess.getServer().getMaxMessageSizeInBytes()) {
			sess.sendResponse("552 5.3.4 Message size exceeds fixed limit");
			return;
		}

		sess.startMailTransaction();

		try {
			sess.getMessageHandler().from(emailAddress);
		} catch (final RejectException ex) {
			// roll back the start of the transaction
			sess.resetMailTransaction();
			sess.sendResponse(ex.getErrorResponse());
			return;
		}

		sess.sendResponse("250 Ok");
	}
}
