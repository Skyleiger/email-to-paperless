package de.dwienzek.emailtopaperless.service;

import com.sun.mail.util.MailSSLSocketFactory;
import de.dwienzek.emailtopaperless.component.ImapConfiguration;
import de.dwienzek.emailtopaperless.util.UsernamePasswordAuthenticator;
import jakarta.mail.Authenticator;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.function.Consumer;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@AllArgsConstructor
public class EmailFetchService {

    private static final Logger LOGGER = LogManager.getLogger(EmailFetchService.class);
    private static final String PROTOCOL = "imap";

    private final ImapConfiguration configuration;

    public void fetchEmails(Consumer<MimeMessage> function) throws MessagingException, GeneralSecurityException {
        LOGGER.info("Fetching emails from imap server '{}'.", configuration.getUrl());

        Properties properties = prepareMailProperties();
        Authenticator authenticator = new UsernamePasswordAuthenticator(configuration.getUsername(), configuration.getPassword());

        Session session = Session.getDefaultInstance(properties, authenticator);

        try (Store store = session.getStore(PROTOCOL)) {
            store.connect(configuration.getUrl(), configuration.getUsername(), configuration.getPassword());

            for (Folder folder : store.getDefaultFolder().list()) {
                fetchEmails(folder, function);
            }
        }
    }

    private void fetchEmails(Folder folder, Consumer<MimeMessage> function) throws MessagingException {
        if (!configuration.isFolderIncluded(folder.getName())) {
            LOGGER.info("Skipping folder '{}', because it's not included (or excluded).", folder.getName());
            return;
        }

        LOGGER.info("Fetching emails from folder '{}'.", folder.getName());

        for (Folder subFolder : folder.list()) {
            fetchEmails(subFolder, function);
        }

        try (folder) {
            folder.open(Folder.READ_WRITE);

            for (Message message : folder.getMessages()) {
                LOGGER.info("Fetching email '{}, {}'.", message.getSubject(), message.getSentDate());
                function.accept((MimeMessage) message);
            }
        }
    }

    private Properties prepareMailProperties() throws GeneralSecurityException {
        Properties properties = new Properties();

        properties.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.imap.socketFactory.fallback", "false");
        properties.put("mail.imap.socketFactory.port", "993");
        properties.put("mail.imap.port", "993");
        properties.put("mail.imap.host", configuration.getUrl());
        properties.put("mail.imap.user", configuration.getUsername());
        properties.put("mail.imap.protocol", PROTOCOL);

        MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
        socketFactory.setTrustAllHosts(true);
        properties.put("mail.imap.ssl.trust", "*");
        properties.put("mail.imap.ssl.socketFactory", socketFactory);

        properties.put("mail.mime.charset", "UTF-8");

        return properties;
    }


}