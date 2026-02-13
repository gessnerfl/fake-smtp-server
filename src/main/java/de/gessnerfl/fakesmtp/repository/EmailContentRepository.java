package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.EmailContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailContentRepository extends JpaRepository<EmailContent, Long> {
}
