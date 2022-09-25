package de.dwienzek.emailtopaperless.component;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Data
public class PaperlessConfiguration {

    private final String url;
    private final String token;
    private final List<String> tags;

    public PaperlessConfiguration(@Value("${paperless.url}") String url,
                                  @Value("${paperless.token}") String token,
                                  @Value("${paperless.tags}") List<String> tags) {
        this.url = url;
        this.token = token;
        this.tags = Collections.unmodifiableList(tags);
    }
}
