package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.server.SmtpServer;
import de.gessnerfl.fakesmtp.server.SmtpServerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

@Profile("default")
@Service
public class SmtpServerFactoryImpl implements SmtpServerFactory {

    private final EmailPersister emailPersister;
    private final SmtpServerConfigurator configurator;

    @Autowired
    public SmtpServerFactoryImpl(EmailPersister emailPersister, SmtpServerConfigurator configurator) {
        this.emailPersister = emailPersister;
        this.configurator = configurator;
    }

    @Override
    public SmtpServer create() {
        SimpleMessageListenerAdapter simpleMessageListenerAdapter = new SimpleMessageListenerAdapter(emailPersister);
        SMTPServer smtpServer = new SMTPServer(simpleMessageListenerAdapter);
        configurator.configure(smtpServer);
        return new SmtpServerImpl(smtpServer);
    }
}
