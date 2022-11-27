package de.dwienzek.emailtopaperless.service.impl;

import de.dwienzek.emailtopaperless.dto.StoredEmail;
import de.dwienzek.emailtopaperless.entity.Email;
import de.dwienzek.emailtopaperless.exception.EmailProcessException;
import de.dwienzek.emailtopaperless.service.EmailProcessService;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@ConditionalOnProperty(value = "email.storing.strategy", havingValue = "FOLDER")
public class LocalFolderProcessServiceImpl implements EmailProcessService {

    private static final Logger LOGGER = LogManager.getLogger(LocalFolderProcessServiceImpl.class);
    private final Path folder;

    public LocalFolderProcessServiceImpl(@Value("${email.storing.folder}") String folder) {
        this.folder = Path.of(folder);
    }

    @PostConstruct
    protected void onPostConstruct() throws IOException {
        LOGGER.info("Using folder storing strategy.");
        Files.createDirectories(folder);
    }

    @Override
    public void processEmail(Email email, StoredEmail storedEmail) throws EmailProcessException {
        LOGGER.info("Copying email '{}' at '{}'.", email.getSubject(), folder);
        Path storedEmailPath = storedEmail.getEmailPath();

        try {
            copyFileToTargetFolder(storedEmailPath);

            try (Stream<Path> stream = Files.list(storedEmail.getAttachmentsDirectoryPath())) {
                for (Path path : stream.toList()) {
                    copyFileToTargetFolder(path);
                }
            }
            
            storedEmail.delete();
        } catch (IOException exception) {
            throw new EmailProcessException(exception);
        }

        LOGGER.info("Email and attachments of '{}' copied to '{}'.", email.getSubject(), folder);
    }

    private void copyFileToTargetFolder(Path file) throws IOException {
        Files.copy(file, folder.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

}