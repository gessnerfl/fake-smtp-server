package de.gessnerfl.fakesmtp.smtp.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("default")
@Service
public class BaseSmtpServerFactory implements SmtpServerFactory {

    private final BaseMessageListener baseMessageListener;
    private final BaseSmtpServerConfigurator configurator;

    @Autowired
    public BaseSmtpServerFactory(BaseMessageListener baseMessageListener, BaseSmtpServerConfigurator configurator) {
        this.baseMessageListener = baseMessageListener;
        this.configurator = configurator;
    }

    @Override
    public SmtpServer create() {
        var simpleMessageListenerAdapter = new MessageListenerAdapter(baseMessageListener);
        var smtpServer = new BaseSmtpServer(simpleMessageListenerAdapter);
        configurator.configure(smtpServer);
        return smtpServer;
    }
}
