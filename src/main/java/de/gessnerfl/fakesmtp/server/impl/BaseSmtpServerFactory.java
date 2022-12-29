package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.server.SmtpServer;
import de.gessnerfl.fakesmtp.server.SmtpServerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import de.gessnerfl.fakesmtp.server.smtp.helper.SimpleMessageListenerAdapter;
import de.gessnerfl.fakesmtp.server.smtp.server.BaseSmtpServer;

@Profile("default")
@Service
public class BaseSmtpServerFactory implements SmtpServerFactory {

    private final MessageListener messageListener;
    private final BaseSmtpServerConfigurator configurator;

    @Autowired
    public BaseSmtpServerFactory(MessageListener messageListener, BaseSmtpServerConfigurator configurator) {
        this.messageListener = messageListener;
        this.configurator = configurator;
    }

    @Override
    public SmtpServer create() {
        var simpleMessageListenerAdapter = new SimpleMessageListenerAdapter(messageListener);
        var smtpServer = new BaseSmtpServer(simpleMessageListenerAdapter);
        configurator.configure(smtpServer);
        return smtpServer;
    }
}
