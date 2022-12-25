package de.gessnerfl.fakesmtp.server.smtp.command;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.gessnerfl.fakesmtp.server.smtp.DropConnectionException;
import de.gessnerfl.fakesmtp.server.smtp.RejectException;
import de.gessnerfl.fakesmtp.server.smtp.io.DotTerminatedInputStream;
import de.gessnerfl.fakesmtp.server.smtp.io.DotUnstuffingInputStream;
import de.gessnerfl.fakesmtp.server.smtp.io.ReceivedHeaderStream;
import de.gessnerfl.fakesmtp.server.smtp.server.BaseCommand;
import de.gessnerfl.fakesmtp.server.smtp.server.Session;

/**
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Jeff Schnitzer
 */
public class DataCommand extends BaseCommand {
	private final static int BUFFER_SIZE = 1024 * 32; // 32k seems reasonable

	public DataCommand() {
		super("DATA", "Following text is collected as the message.\n" + "End data with <CR><LF>.<CR><LF>");
	}

	@Override
	public void execute(final String commandString, final Session sess) throws IOException, DropConnectionException {
		if (!sess.isMailTransactionInProgress()) {
			sess.sendResponse("503 5.5.1 Error: need MAIL command");
			return;
		}
		if (sess.getRecipientCount() == 0) {
			sess.sendResponse("503 Error: need RCPT command");
			return;
		}

		sess.sendResponse("354 End data with <CR><LF>.<CR><LF>");

		InputStream stream = sess.getRawInput();
		stream = new BufferedInputStream(stream, BUFFER_SIZE);
		stream = new DotTerminatedInputStream(stream);
		stream = new DotUnstuffingInputStream(stream);
		if (!sess.getServer().getDisableReceivedHeaders()) {
			stream = new ReceivedHeaderStream(stream,
					sess.getHelo(),
					sess.getRemoteAddress().getAddress(),
					sess.getServer().getHostName(),
					sess.getServer().getSoftwareName(),
					sess.getSessionId(),
					sess.getSingleRecipient());
		}

		try {
			sess.getMessageHandler().data(stream);

			// Just in case the handler didn't consume all the data, we might as well
			// suck it up so it doesn't pollute further exchanges. This code used to
			// throw an exception, but this seems an arbitrary part of the contract that
			// we might as well relax.
			while (stream.read() != -1) {}
		} catch (final DropConnectionException ex) {
			throw ex; // Propagate this
		} catch (final RejectException ex) {
			sess.sendResponse(ex.getErrorResponse());
			return;
		}

		sess.sendResponse("250 Ok");
		sess.resetMailTransaction();
	}
}
