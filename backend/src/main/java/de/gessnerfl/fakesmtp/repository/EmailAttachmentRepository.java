package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.EmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment,Long> {

}
