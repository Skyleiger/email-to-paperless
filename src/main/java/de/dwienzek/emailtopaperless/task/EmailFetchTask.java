package de.dwienzek.emailtopaperless.task;

import de.dwienzek.emailtopaperless.service.EmailFetchService;
import de.dwienzek.emailtopaperless.component.EmailProcessFunction;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;

@Component
@RequiredArgsConstructor
public class EmailFetchTask {

    private static final Logger LOGGER = LogManager.getLogger(EmailFetchTask.class);
    private final EmailFetchService emailFetchService;
    private final EmailProcessFunction emailProcessFunction;

    @Scheduled(cron = "${task.fetch-interval:}#{'0 * * ? * *'}")
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
