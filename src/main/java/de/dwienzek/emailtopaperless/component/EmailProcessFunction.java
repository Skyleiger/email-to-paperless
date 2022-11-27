package de.dwienzek.emailtopaperless.component;

import de.dwienzek.emailtopaperless.dto.StoredEmail;
import de.dwienzek.emailtopaperless.entity.Email;
import de.dwienzek.emailtopaperless.repository.EmailRepository;
import de.dwienzek.emailtopaperless.service.EmailProcessService;
import de.dwienzek.emailtopaperless.service.EmailStoreService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Component
public class EmailProcessFunction implements Consumer<MimeMessage> {

    private static final Logger LOGGER = LogManager.getLogger(EmailProcessFunction.class);
    private final EmailStoreService emailStoreService;
    private final EmailProcessService emailProcessService;
    private final EmailRepository emailRepository;

    @Override
    public void accept(MimeMessage message) {
        Email email = null;
        StoredEmail storedEmail = null;

        try {
            email = new Email(message);

            if (emailRepository.existsBySubjectAndSentDate(email.getSubject(), email.getSentDate())) {
                LOGGER.debug("Skipping email '{}', because it was already processed in the past.", email);
                return;
            }

            storedEmail = emailStoreService.storeEmail(message);
            emailProcessService.processEmail(email, storedEmail);
            emailRepository.save(email);
        } catch (Exception exception) {
            LOGGER.error(new ParameterizedMessage("Error while processing email '{}'.", email), exception);
        } finally {
            if (storedEmail != null) {
                try {
                    storedEmail.delete();
                } catch (IOException exception) {
                    LOGGER.error(new ParameterizedMessage("Failed to delete stored email '{}'.", storedEmail), exception);
                }
            }
        }
    }

}