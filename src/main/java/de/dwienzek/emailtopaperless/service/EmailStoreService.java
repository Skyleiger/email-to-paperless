package de.dwienzek.emailtopaperless.service;

import de.dwienzek.emailtopaperless.dto.StoredEmail;
import de.dwienzek.emailtopaperless.exception.EmailStoreException;
import jakarta.mail.internet.MimeMessage;

public interface EmailStoreService {

    StoredEmail storeEmail(MimeMessage message) throws EmailStoreException;

}