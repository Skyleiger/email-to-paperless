package de.dwienzek.emailtopaperless.service.impl;

import de.dwienzek.emailtopaperless.component.PaperlessConfiguration;
import de.dwienzek.emailtopaperless.dto.StoredEmail;
import de.dwienzek.emailtopaperless.entity.Email;
import de.dwienzek.emailtopaperless.exception.EmailProcessException;
import de.dwienzek.emailtopaperless.service.EmailProcessService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.stream.Stream;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@ConditionalOnProperty(
        value = "email.storing.strategy",
        havingValue = "PAPERLESS",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class PaperlessProcessServiceImpl implements EmailProcessService {

    private static final Logger LOGGER = LogManager.getLogger(PaperlessProcessServiceImpl.class);
    private static final String CSRF_TOKEN_FETCH_ENDPOINT = "/api";
    private static final String DOCUMENT_POST_API_ENDPOINT = "/api/documents/post_document/";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    private final PaperlessConfiguration paperlessConfiguration;
    private Cookie csrfCookie;

    @PostConstruct
    protected void onPostConstruct() {
        LOGGER.info("Using folder storing strategy.");
    }

    @Override
    public void processEmail(Email email, StoredEmail storedEmail) throws EmailProcessException {
        try {
            LOGGER.info("Uploading email '{}' to paperless.", email.getSubject());

            String url = parseUrl(DOCUMENT_POST_API_ENDPOINT);

            Path emailPath = storedEmail.getEmailPath();
            if (emailPath != null) {
                uploadDocument(url, email.getSubject(), email.getSentDate(), emailPath);
            }

            for (Path attachment : getAttachments(storedEmail)) {
                uploadDocument(url, attachment.getFileName().toString(), email.getSentDate(), attachment);
            }

            LOGGER.info("Upload to paperless finished.");
        } catch (IOException | ParseException exception) {
            throw new EmailProcessException(exception);
        }
    }

    private List<Path> getAttachments(StoredEmail storedEmail) throws IOException {
        try (Stream<Path> stream = Files.list(storedEmail.getAttachmentsDirectoryPath())) {
            return stream.toList();
        }
    }

    private void uploadDocument(String url, String title, Instant timestamp,
                                Path path) throws IOException, ParseException {
        LOGGER.debug("Upload document [path={}, title={}, timestamp={}] to '{}'.", path, title, timestamp, url);

        refreshCSRFCookieIfRequired();

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             InputStream inputStream = Files.newInputStream(path)) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Authorization", "Token " + paperlessConfiguration.getToken());
            httpPost.addHeader("X-CSRF-TOKEN", csrfCookie.getValue());

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("title", title);
            builder.addTextBody("created", DATE_TIME_FORMATTER.format(timestamp));

            for (String tag : paperlessConfiguration.getTags()) {
                builder.addTextBody("tags", tag);
            }

            builder.addBinaryBody(
                    "document",
                    inputStream,
                    ContentType.APPLICATION_OCTET_STREAM,
                    path.getFileName().toString()
            );

            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String content = EntityUtils.toString(response.getEntity());

                if (!content.equals("\"OK\"")) {
                    LOGGER.warn("Response of document upload endpoint '{}' returned an illegal response: {}", url, content);
                    return;
                }

            }

            LOGGER.debug("Upload of document finished.");
        } catch (IOException | ParseException exception) {
            LOGGER.debug("Upload of document failed.", exception);
            throw exception;
        }
    }

    private void refreshCSRFCookieIfRequired() throws IOException {
        if (csrfCookie == null || csrfCookie.getExpiryDate().toInstant().isBefore(Instant.now())) {
            LOGGER.info("CSRF-Cookie is expired.");
            refreshCSRFCookie();
        }
    }

    private void refreshCSRFCookie() throws IOException {
        LOGGER.info("Refreshing CSRF-Cookie.");

        String url = parseUrl(CSRF_TOKEN_FETCH_ENDPOINT);

        CookieStore cookieStore = new BasicCookieStore();
        HttpClientContext context = HttpClientContext.create();
        context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Authorization", "Token " + paperlessConfiguration.getToken());

            try (CloseableHttpResponse ignored = httpClient.execute(httpGet, context)) {
                cookieStore = context.getCookieStore();
                csrfCookie = cookieStore.getCookies()
                        .stream()
                        .filter(cookie -> cookie.getName().equals("csrftoken"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("no 'csrftoken' cookie found"));
            }
        }

        LOGGER.info("CSRF-Cookie refreshed.");
    }

    private String parseUrl(String endpoint) {
        String url = paperlessConfiguration.getUrl();

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url + endpoint;
    }

}