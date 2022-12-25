package de.gessnerfl.fakesmtp.server;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class EmailServer {

    private final Logger logger;
    private final SmtpServerFactory smtpServerFactory;

    SmtpServer smtpServer;

    @Autowired
    public EmailServer(SmtpServerFactory smtpServerFactory, Logger logger) {
        this.smtpServerFactory = smtpServerFactory;
        this.logger = logger;
    }

    @PostConstruct
    public void startServer() {
        smtpServer = smtpServerFactory.create();
        smtpServer.start();
    }

    @PreDestroy
    public void shutdown() {
        if (smtpServer != null) {
            logger.info("Stop SMTP server");
            smtpServer.stop();
            logger.info("SMTP server stopped");
        } else {
            logger.debug("SMTP server not started; shutdown not required");
        }
    }

}
