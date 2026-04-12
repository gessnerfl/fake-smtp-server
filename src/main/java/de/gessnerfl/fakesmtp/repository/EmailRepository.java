package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long>, JpaSpecificationExecutor<Email> {

    @Query(value = """
            SELECT id
            FROM email
            ORDER BY received_on DESC, id DESC
            OFFSET ?1
            """, nativeQuery = true)
    List<Long> findEmailIdsExceedingLimit(int maxNumber);

    @Modifying
    @Query(value = "DELETE FROM email_content WHERE email IN (:emailIds)", nativeQuery = true)
    int deleteEmailContentByEmailIds(@Param("emailIds") List<Long> emailIds);

    @Modifying
    @Query(value = "DELETE FROM email_attachment WHERE email IN (:emailIds)", nativeQuery = true)
    int deleteEmailAttachmentsByEmailIds(@Param("emailIds") List<Long> emailIds);

    @Modifying
    @Query(value = "DELETE FROM email_inline_image WHERE email IN (:emailIds)", nativeQuery = true)
    int deleteEmailInlineImagesByEmailIds(@Param("emailIds") List<Long> emailIds);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM email WHERE id IN (:emailIds)", nativeQuery = true)
    int deleteEmailsByEmailIds(@Param("emailIds") List<Long> emailIds);

    @Transactional
    default int deleteEmailsExceedingDateRetentionLimit(int maxNumber) {
        Logger logger = LoggerFactory.getLogger(EmailRepository.class);
        List<Long> emailIds = findEmailIdsExceedingLimit(maxNumber);
        if (emailIds.isEmpty()) {
            return 0;
        }

        int contentDeleted = deleteEmailContentByEmailIds(emailIds);
        int attachmentsDeleted = deleteEmailAttachmentsByEmailIds(emailIds);
        int inlineImagesDeleted = deleteEmailInlineImagesByEmailIds(emailIds);
        int emailsDeleted = deleteEmailsByEmailIds(emailIds);

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
