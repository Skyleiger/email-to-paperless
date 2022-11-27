package de.dwienzek.emailtopaperless.service;

import de.dwienzek.emailtopaperless.exception.EmailFetchException;
import jakarta.mail.internet.MimeMessage;

import java.util.function.Consumer;

public interface EmailFetchService {

    void fetchEmails(Consumer<MimeMessage> function) throws EmailFetchException;

}