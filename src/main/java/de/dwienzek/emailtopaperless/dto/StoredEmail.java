package de.dwienzek.emailtopaperless.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

@Value
public class StoredEmail {

    @Getter(AccessLevel.NONE)
    Path emailFolderPath;
    Path emailPath;
    Path attachmentsDirectoryPath;

    public void delete() throws IOException {
        FileUtils.deleteDirectory(emailFolderPath.toFile());
    }

}
