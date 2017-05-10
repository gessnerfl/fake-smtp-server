package de.gessnerfl.fakesmtp.repository;

import de.gessnerfl.fakesmtp.model.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends JpaRepository<Email,String>{
}
