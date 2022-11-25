package de.dwienzek.emailtopaperless.component;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Data
public class ImapConfiguration {

    private final String url;
    private final String username;
    private final String password;
    private final List<String> includedFolders;
    private final List<String> excludedFolders;

    public ImapConfiguration(@Value("${imap.url}") String url,
                             @Value("${imap.username}") String username,
                             @Value("${imap.password}") String password,
                             @Value("${imap.includedFolders}") List<String> includedFolders,
                             @Value("${imap.excludedFolders}") List<String> excludedFolders) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.includedFolders = Collections.unmodifiableList(includedFolders);
        this.excludedFolders = Collections.unmodifiableList(excludedFolders);
    }

    public boolean isFolderIncluded(String folder) {
        return (includedFolders.isEmpty() || includedFolders.contains(folder)) && !excludedFolders.contains(folder);
    }

}