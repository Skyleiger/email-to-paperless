package de.dwienzek.emailtopaperless.entity;

import lombok.*;
import org.hibernate.Hibernate;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Email {

    @Id
    @GeneratedValue
    private Long id;
    private String subject;
    private Instant sentDate;

    public Email(MimeMessage message) throws MessagingException {
        subject = message.getSubject();
        sentDate = message.getSentDate().toInstant();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Email email = (Email) o;
        return id != null && Objects.equals(id, email.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
