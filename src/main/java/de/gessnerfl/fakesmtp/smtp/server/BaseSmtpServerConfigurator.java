package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import de.gessnerfl.fakesmtp.smtp.auth.BasicUsernamePasswordValidator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import de.gessnerfl.fakesmtp.smtp.auth.EasyAuthenticationHandlerFactory;

@Service
public class BaseSmtpServerConfigurator {

    private final FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    private final BasicUsernamePasswordValidator basicUsernamePasswordValidator;
    private final Logger logger;

    @Autowired
    public BaseSmtpServerConfigurator(FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties, BasicUsernamePasswordValidator basicUsernamePasswordValidator, Logger logger) {
        this.fakeSmtpConfigurationProperties = fakeSmtpConfigurationProperties;
        this.basicUsernamePasswordValidator = basicUsernamePasswordValidator;
        this.logger = logger;
    }

    public void configure(BaseSmtpServer smtpServer) {
        smtpServer.setPort(fakeSmtpConfigurationProperties.getPort());
        smtpServer.setBindAddress(fakeSmtpConfigurationProperties.getBindAddress());
        if (fakeSmtpConfigurationProperties.getAuthentication() != null) {
            configureAuthentication(smtpServer, fakeSmtpConfigurationProperties.getAuthentication());
        }
    }

    private void configureAuthentication(BaseSmtpServer smtpServer, FakeSmtpConfigurationProperties.Authentication authentication) {
        if (!StringUtils.hasText(authentication.getUsername())) {
            logger.error("Username is missing; skip configuration of authentication");
        } else if (!StringUtils.hasText(authentication.getPassword())) {
            logger.error("Password is missing; skip configuration of authentication");
        } else {
            logger.info("Setup simple username and password authentication for SMTP server");
            smtpServer.setAuthenticationHandlerFactory(new EasyAuthenticationHandlerFactory(basicUsernamePasswordValidator));
        }
    }
}
