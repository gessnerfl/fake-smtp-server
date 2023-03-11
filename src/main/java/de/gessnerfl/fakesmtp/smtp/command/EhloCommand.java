package de.gessnerfl.fakesmtp.smtp.command;

import java.io.IOException;
import java.util.List;

import de.gessnerfl.fakesmtp.smtp.AuthenticationHandlerFactory;
import de.gessnerfl.fakesmtp.smtp.server.Session;

public class EhloCommand extends BaseCommand {
    public EhloCommand() {
        super(CommandVerb.EHLO, "Introduce yourself.", "<hostname>");
    }

    @Override
    public void execute(final String commandString, final Session sess) throws IOException {
        final String[] args = this.getArgs(commandString);
        if (args.length < 2) {
            sess.sendResponse("501 Syntax: EHLO hostname");
            return;
        }

        sess.resetMailTransaction();
        sess.setHelo(args[1]);

        // postfix returns...
        // 250-server.host.name
        // 250-PIPELINING
        // 250-SIZE 10240000
        // 250-ETRN
        // 250 8BITMIME

        // Once upon a time this code tracked whether or not HELO/EHLO has been seen
        // already and gave an error msg. However, this is stupid and pointless.
        // Postfix doesn't care, so we won't either. If you want more, read:
        // http://homepages.tesco.net/J.deBoynePollard/FGA/smtp-avoid-helo.html

        final StringBuilder response = new StringBuilder();

        response.append("250-")
                .append(sess.getServer().getHostName())
                .append("\r\n")
                .append("250-8BITMIME");

        final long maxSize = sess.getServer().getMaxMessageSizeInBytes();
        if (maxSize > 0) {
            response.append("\r\n").append("250-SIZE ").append(maxSize);
        }

        // Enabling / Hiding TLS is a server setting
        if (sess.getServer().getEnableTLS()) {
            response.append("\r\n").append("250-STARTTLS");
        }

        // Check to see if we support authentication
        final AuthenticationHandlerFactory authFact = sess.getServer().getAuthenticationHandlerFactory();
        if (authFact != null) {
            final List<String> supportedMechanisms = authFact.getAuthenticationMechanisms();
            if (!supportedMechanisms.isEmpty()) {
                response.append("\r\n")
                        .append("250-")
                        .append(CommandVerb.AUTH.name())
                        .append(" ")
                        .append(String.join(" ", supportedMechanisms));
            }
        }

        response.append("\r\n").append("250 Ok");

        sess.sendResponse(response.toString());
    }
}
