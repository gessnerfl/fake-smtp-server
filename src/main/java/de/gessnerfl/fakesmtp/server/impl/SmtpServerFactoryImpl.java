package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
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

    private final FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    private final EmailPersister emailPersister;

    @Autowired
    public SmtpServerFactoryImpl(FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties, EmailPersister emailPersister) {
        this.fakeSmtpConfigurationProperties = fakeSmtpConfigurationProperties;
        this.emailPersister = emailPersister;
    }

    @Override
    public SmtpServer create(){
        SimpleMessageListenerAdapter simpleMessageListenerAdapter = new SimpleMessageListenerAdapter(emailPersister);
        //TODO Add authentication
        SMTPServer smtpServer = new SMTPServer(simpleMessageListenerAdapter);
        smtpServer.setPort(fakeSmtpConfigurationProperties.getPort());
        smtpServer.setBindAddress(fakeSmtpConfigurationProperties.getBindAddress());
        return new SmtpServerImpl(smtpServer);
    }
}
