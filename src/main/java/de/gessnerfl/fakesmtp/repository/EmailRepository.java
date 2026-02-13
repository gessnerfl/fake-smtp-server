package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long>, JpaSpecificationExecutor<Email> {

    @Modifying
    @Query(value = "DELETE FROM email_content WHERE email IN (SELECT id FROM email ORDER BY received_on DESC OFFSET ?1)", nativeQuery = true)
    int deleteEmailContentExceedingLimit(int maxNumber);

    @Modifying
    @Query(value = "DELETE FROM email_attachment WHERE email IN (SELECT id FROM email ORDER BY received_on DESC OFFSET ?1)", nativeQuery = true)
    int deleteEmailAttachmentsExceedingLimit(int maxNumber);

    @Modifying
    @Query(value = "DELETE FROM email_inline_image WHERE email IN (SELECT id FROM email ORDER BY received_on DESC OFFSET ?1)", nativeQuery = true)
    int deleteEmailInlineImagesExceedingLimit(int maxNumber);

    @Modifying
    @Query(value = "DELETE FROM email WHERE id IN (SELECT id FROM email ORDER BY received_on DESC OFFSET ?1)", nativeQuery = true)
    int deleteEmailsExceedingLimit(int maxNumber);

    @Transactional
    default int deleteEmailsExceedingDateRetentionLimit(int maxNumber) {
        Logger logger = LoggerFactory.getLogger(EmailRepository.class);

        int contentDeleted = deleteEmailContentExceedingLimit(maxNumber);
        int attachmentsDeleted = deleteEmailAttachmentsExceedingLimit(maxNumber);
        int inlineImagesDeleted = deleteEmailInlineImagesExceedingLimit(maxNumber);
        int emailsDeleted = deleteEmailsExceedingLimit(maxNumber);

        if (logger.isDebugEnabled()) {
            logger.debug("Deleted {} emails exceeding retention limit of {}. Details: {} content records, {} attachments, {} inline images deleted",
                    emailsDeleted, maxNumber, contentDeleted, attachmentsDeleted, inlineImagesDeleted);
        }

        return emailsDeleted;
    }

    @Transactional
    @Query
    List<Email> findBySubject(String subject);

}
