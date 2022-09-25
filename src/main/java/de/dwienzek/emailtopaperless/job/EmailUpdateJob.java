package de.dwienzek.emailtopaperless.job;

import de.dwienzek.emailtopaperless.component.EmailProcessFunction;
import de.dwienzek.emailtopaperless.service.EmailFetchService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class EmailUpdateJob {

    private static final Logger LOGGER = LogManager.getLogger(EmailUpdateJob.class);
    private final EmailFetchService emailFetchService;
    private final EmailProcessFunction emailProcessFunction;

    @Scheduled(timeUnit = TimeUnit.MINUTES, fixedDelayString = "${email.update-interval}")
    public void fetchEmailsAndConvertToPaperless() {
        LOGGER.info("Processing emails from imap server.");

        try {
            emailFetchService.fetchEmails(emailProcessFunction);
        } catch (MessagingException | GeneralSecurityException exception) {
            LOGGER.error("Failed to fetch emails from imap server.", exception);
        }

        LOGGER.info("Email processing done.");
    }

}
