package de.dwienzek.emailtopaperless.component;

import de.dwienzek.emailtopaperless.dto.StoredEmail;
import de.dwienzek.emailtopaperless.entity.Email;
import de.dwienzek.emailtopaperless.repository.EmailRepository;
import de.dwienzek.emailtopaperless.service.EmailStoreService;
import de.dwienzek.emailtopaperless.service.EmailUploadService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Component
public class EmailProcessFunction implements Consumer<MimeMessage> {

    private static final Logger LOGGER = LogManager.getLogger(EmailProcessFunction.class);
    private final EmailStoreService emailStoreService;
    private final EmailUploadService emailUploadService;
    private final EmailRepository emailRepository;

    @SneakyThrows(value = InterruptedException.class)
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
            emailUploadService.uploadEmail(email, storedEmail);
            emailRepository.save(email);
        } catch (InterruptedException exception) {
            throw exception;
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
