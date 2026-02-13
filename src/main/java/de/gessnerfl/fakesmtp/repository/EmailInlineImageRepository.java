package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.InlineImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailInlineImageRepository extends JpaRepository<InlineImage, Long> {
}
