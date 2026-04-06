package de.gessnerfl.fakesmtp.smtp.command;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.gessnerfl.fakesmtp.smtp.RejectException;
import de.gessnerfl.fakesmtp.smtp.io.DotTerminatedInputStream;
import de.gessnerfl.fakesmtp.smtp.io.DotUnstuffingInputStream;
import de.gessnerfl.fakesmtp.smtp.io.MaxMessageSizeExceededException;
import de.gessnerfl.fakesmtp.smtp.io.MaxMessageSizeInputStream;
import de.gessnerfl.fakesmtp.smtp.io.ReceivedHeaderStream;
import de.gessnerfl.fakesmtp.smtp.server.Session;

public class DataCommand extends BaseCommand {
    private static final int BUFFER_SIZE = 1024 * 32; // 32k seems reasonable
    private static final String MESSAGE_SIZE_EXCEEDED_RESPONSE = "552 5.3.4 Message size exceeds fixed limit";

    public DataCommand() {
        super(CommandVerb.DATA, "Following text is collected as the message.\n" + "End data with <CR><LF>.<CR><LF>");
    }

    @Override
    public void execute(final String commandString, final Session sess) throws IOException {
        if (!sess.isMailTransactionInProgress()) {
            sess.sendResponse("503 5.5.1 Error: need MAIL command");
            return;
        }
        if (sess.getRecipientCount() == 0) {
            sess.sendResponse("503 Error: need RCPT command");
            return;
        }

        sess.sendResponse("354 End data with <CR><LF>.<CR><LF>");

        final var dataStreamContext = buildReceivedHeaderStream(sess);
        try {
            sess.getMessageHandler().data(dataStreamContext.receivedHeaderStream());
            while (dataStreamContext.receivedHeaderStream().read() != -1) {
                // Just in case the handler didn't consume all the data, we might as well
                // suck it up so it doesn't pollute further exchanges.  This code used to
                // throw an exception, but this seems an arbitrary part of the contract that
                // we might as well relax.
            }
        } catch (final RejectException ex) {
            if (!drainRemainingData(dataStreamContext.dotTerminatedInputStream())) {
                sess.quit();
                return;
            }
            sess.sendResponse(ex.getErrorResponse());
            sess.resetMailTransaction();
            return;
        } catch (final MaxMessageSizeExceededException ex) {
            if (!drainRemainingData(dataStreamContext.dotTerminatedInputStream())) {
                sess.quit();
                return;
            }
            sess.sendResponse(MESSAGE_SIZE_EXCEEDED_RESPONSE);
            sess.resetMailTransaction();
            return;
        }

        sess.sendResponse("250 Ok");
        sess.resetMailTransaction();
    }

    private static DataStreamContext buildReceivedHeaderStream(Session sess) {
        final var bis = new BufferedInputStream(sess.getRawInput(), BUFFER_SIZE);
        final var btis = new DotTerminatedInputStream(bis);
        final var duis = new DotUnstuffingInputStream(btis);
        final var payloadStream = createPayloadStream(sess, duis);
        final var receivedHeaderStream = new ReceivedHeaderStream(payloadStream,
                sess.getHelo(),
                sess.getRemoteAddress().getAddress(),
                sess.getServer().getHostName(),
                sess.getServer().getSoftwareName(),
                sess.getSessionId(),
                sess.getSingleRecipient());
        return new DataStreamContext(btis, receivedHeaderStream);
    }

    private static InputStream createPayloadStream(Session sess, DotUnstuffingInputStream duis) {
        final long maxMessageSizeInBytes = sess.getServer().getMaxMessageSizeInBytes();
        if (maxMessageSizeInBytes <= 0) {
            return duis;
        }
        return new MaxMessageSizeInputStream(duis, maxMessageSizeInBytes);
    }

    private static boolean drainRemainingData(DotTerminatedInputStream dotTerminatedInputStream) {
        try {
            while (dotTerminatedInputStream.read() != -1) {
                // Drain the remainder of the current DATA payload so the next command is parsed correctly.
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private record DataStreamContext(
            DotTerminatedInputStream dotTerminatedInputStream,
            ReceivedHeaderStream receivedHeaderStream
    ) {
    }
}
