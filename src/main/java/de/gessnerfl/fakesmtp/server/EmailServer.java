package de.gessnerfl.fakesmtp.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class EmailServer {

    private final SmtpServerFactory smtpServerFactory;

    SmtpServer smtpServer;

    @Autowired
    public EmailServer(SmtpServerFactory smtpServerFactory) {
        this.smtpServerFactory = smtpServerFactory;
    }

    @PostConstruct
    public void startServer() {
        smtpServer = smtpServerFactory.create();
        smtpServer.start();
    }

    @PreDestroy
    public void shutdown() {
        if (smtpServer != null) {
            smtpServer.stop();
        }
    }

}
