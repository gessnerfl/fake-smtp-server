package de.gessnerfl.fakesmtp.service;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.gessnerfl.fakesmtp.controller.EmailNotFoundException;
import de.gessnerfl.fakesmtp.repository.EmailAttachmentRepository;
import de.gessnerfl.fakesmtp.repository.EmailContentRepository;
import de.gessnerfl.fakesmtp.repository.EmailInlineImageRepository;
import de.gessnerfl.fakesmtp.repository.EmailRepository;

@Service
public class EmailDeletionService {

    private final EmailRepository emailRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final EmailContentRepository emailContentRepository;
    private final EmailInlineImageRepository emailInlineImageRepository;
    private final Logger logger;

    @Autowired
    public EmailDeletionService(EmailRepository emailRepository,
            EmailAttachmentRepository emailAttachmentRepository,
            EmailContentRepository emailContentRepository,
            EmailInlineImageRepository emailInlineImageRepository,
            Logger logger) {
        this.emailRepository = emailRepository;
        this.emailAttachmentRepository = emailAttachmentRepository;
        this.emailContentRepository = emailContentRepository;
        this.emailInlineImageRepository = emailInlineImageRepository;
        this.logger = logger;
    }

    @Transactional
    public void deleteEmailById(Long id) {
        var email = emailRepository.findById(id)
                .orElseThrow(() -> new EmailNotFoundException("Could not find email " + id));
        emailRepository.delete(email);
        emailRepository.flush();
        logger.debug("Email with id {} deleted", id);
    }

    @Transactional
    public void deleteAllEmails() {
        emailAttachmentRepository.deleteAllInBatch();
        emailContentRepository.deleteAllInBatch();
        emailInlineImageRepository.deleteAllInBatch();
        emailRepository.deleteAllInBatch();
        emailRepository.flush();
        logger.info("All emails deleted");
    }
}
