package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.repository.EmailRepository;
import de.gessnerfl.fakesmtp.service.EmailSseEmitterService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = IOException.class)
public class BaseMessageListener implements MessageListener {
    private final BlockedRecipientAddresses blockedRecipientAddresses;
    private final EmailFactory emailFactory;
    private final EmailFilter emailFilter;
    private final EmailRepository emailRepository;
    private final MessageForwarder messageForwarder;
    private final EmailSseEmitterService emailSseEmitterService;
    private final Logger logger;

    @Autowired
    public BaseMessageListener(BlockedRecipientAddresses blockedRecipientAddresses,
                               EmailFactory emailFactory,
                               EmailFilter emailFilter,
                               EmailRepository emailRepository,
                               MessageForwarder messageForwarder,
                               EmailSseEmitterService emailSseEmitterService,
                               Logger logger) {
        this.blockedRecipientAddresses = blockedRecipientAddresses;
        this.emailFactory = emailFactory;
        this.emailFilter = emailFilter;
        this.emailRepository = emailRepository;
        this.messageForwarder = messageForwarder;
        this.emailSseEmitterService = emailSseEmitterService;
        this.logger = logger;
    }

    @Override
    public boolean accept(String from, String recipient) {
        return !blockedRecipientAddresses.isBlocked(recipient);
    }

    @Override
    public void deliver(String sender, String recipient, InputStream data) throws IOException {
        logger.debug("Received email from {} for {}", sender, recipient);

        var rawData = new RawData(sender, recipient, IOUtils.toByteArray(data));

        if(!emailFilter.ignore(sender,recipient)) {
            var email = emailFactory.convert(rawData);
            email = emailRepository.save(email);

            emailSseEmitterService.sendEmailReceivedEvent(email);

            messageForwarder.forward(rawData);
        }
    }
}
