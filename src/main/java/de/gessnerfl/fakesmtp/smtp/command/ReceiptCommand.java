package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;
import java.util.Locale;

import de.gessnerfl.fakesmtp.smtp.RejectException;
import de.gessnerfl.fakesmtp.smtp.server.Session;
import de.gessnerfl.fakesmtp.smtp.util.EmailUtils;

public class ReceiptCommand extends BaseCommand {
	public ReceiptCommand() {
		super(CommandVerb.RCPT, "Specifies the recipient. Can be used any number of times.", "TO: <recipient> [ <parameters> ]");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException {
		if (!sess.isMailTransactionInProgress()) {
			sess.sendResponse("503 5.5.1 Error: need MAIL command");
			return;
		}

		final String args = this.getArgPredicate(commandString);
		if (!args.toUpperCase(Locale.ENGLISH).startsWith("TO:")) {
			sess.sendResponse("501 Syntax: RCPT TO: <address>  Error in parameters: \"" + args + "\"");
		}
		final String recipientAddress = EmailUtils.extractEmailAddress(args, 3);
		try {
			sess.getMessageHandler().recipient(recipientAddress);
			sess.addRecipient(recipientAddress);
			sess.sendResponse("250 Ok");
		} catch (final RejectException ex) {
			sess.sendResponse(ex.getErrorResponse());
		}
	}
}
