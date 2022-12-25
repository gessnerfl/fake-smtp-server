package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.server.SmtpServer;
import de.gessnerfl.fakesmtp.server.smtp.server.SMTPServer;

public class SmtpServerImpl implements SmtpServer {

    final SMTPServer smtpServer;

    SmtpServerImpl(SMTPServer smtpServer) {
        this.smtpServer = smtpServer;
    }

    @Override
    public void start() {
        smtpServer.start();
    }

    @Override
    public void stop() {
        smtpServer.stop();
    }
}
