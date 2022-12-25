package de.gessnerfl.fakesmtp.server.impl;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;

@Service
public class MessageForwarder {

    private final FakeSmtpConfigurationProperties configurationProperties;
    private final JavaMailSenderFacade javaMailSenderFacade;
    private final Logger logger;

    @Autowired
    public MessageForwarder(FakeSmtpConfigurationProperties configurationProperties, JavaMailSenderFacade javaMailSenderFacade, Logger logger) {
        this.configurationProperties = configurationProperties;
        this.javaMailSenderFacade = javaMailSenderFacade;
        this.logger = logger;
    }

    public void forward(RawData rawData){
        if(configurationProperties.isForwardEmails()){
            logger.info("Forward message to configured target email system");
            try {
                javaMailSenderFacade.send(rawData.toMimeMessage());
            } catch (MessagingException e) {
                logger.warn("Failed to convert raw data to MimeMessage; fall back to simple message forwarding", e);
                var message = new SimpleMailMessage();
                message.setFrom(rawData.getFrom());
                message.setTo(rawData.getTo());
                message.setText(rawData.getContentAsString());
                javaMailSenderFacade.send(message);
            }
        }
    }
}
