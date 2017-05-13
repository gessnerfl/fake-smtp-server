package de.gessnerfl.fakesmtp.service;

import de.gessnerfl.fakesmtp.config.FakeSmtpConfigurationProperties;
import de.gessnerfl.fakesmtp.repository.EmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class EmailRetentionTimer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailRetentionTimer.class);

    private final FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties;
    private final EmailRepository emailRepository;

    @Autowired
    public EmailRetentionTimer(FakeSmtpConfigurationProperties fakeSmtpConfigurationProperties, EmailRepository emailRepository) {
        this.fakeSmtpConfigurationProperties = fakeSmtpConfigurationProperties;
        this.emailRepository = emailRepository;
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 500)
    public void deleteOutdatedMails(){
        FakeSmtpConfigurationProperties.Persistence persistence = fakeSmtpConfigurationProperties.getPersistence();
        if(isDataRetentionConfigured(persistence)){
            int maxNumber = persistence.getMaxNumberEmails();
            int count = emailRepository.deleteEmailsExceedingDateRetentionLimit(maxNumber);
            LOGGER.info("Deleted {} emails which exceeded the maximum number {} of emails to be stored", count, maxNumber);
        }
    }

    private boolean isDataRetentionConfigured(FakeSmtpConfigurationProperties.Persistence persistence) {
        return persistence != null && persistence.getMaxNumberEmails() != null && persistence.getMaxNumberEmails() > 0;
    }

}
