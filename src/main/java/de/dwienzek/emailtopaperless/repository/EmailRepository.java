package de.dwienzek.emailtopaperless.repository;

import de.dwienzek.emailtopaperless.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface EmailRepository extends JpaRepository<Email, Long> {

    Email findBySubjectAndSentDate(String subject, Instant sentDate);

    boolean existsBySubjectAndSentDate(String subject, Instant sentDate);

}
