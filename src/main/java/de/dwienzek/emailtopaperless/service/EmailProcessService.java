package de.dwienzek.emailtopaperless.service;

import de.dwienzek.emailtopaperless.dto.StoredEmail;
import de.dwienzek.emailtopaperless.entity.Email;
import de.dwienzek.emailtopaperless.exception.EmailProcessException;

public interface EmailProcessService {

    void processEmail(Email email, StoredEmail storedEmail) throws EmailProcessException;

}