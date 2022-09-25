package de.dwienzek.emailtopaperless.component;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Data
public class ImapConfiguration {

    private String url;
    private String username;
    private String password;
    private List<String> excludedFolders;

    public ImapConfiguration(@Value("${imap.url}") String url,
                             @Value("${imap.username}") String username,
                             @Value("${imap.password}") String password,
                             @Value("${imap.excludedFolders}") List<String> excludedFolders) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.excludedFolders = Collections.unmodifiableList(excludedFolders);
    }

}
