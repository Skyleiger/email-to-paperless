package de.dwienzek.emailtopaperless.service;

import com.google.common.html.HtmlEscapers;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import de.dwienzek.emailtopaperless.dto.MimeBody;
import de.dwienzek.emailtopaperless.dto.MimeHeaders;
import de.dwienzek.emailtopaperless.dto.MimeInlineImage;
import de.dwienzek.emailtopaperless.dto.StoredEmail;
import de.dwienzek.emailtopaperless.util.ExceptionBiConsumer;
import de.dwienzek.emailtopaperless.util.StringReplacer;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MimeTypes;
import org.simplejavamail.api.email.AttachmentResource;
import org.simplejavamail.converter.EmailConverter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class EmailStoreService {

    private static final Logger LOGGER = LogManager.getLogger(EmailStoreService.class);
    // html wrapper template for text/plain messages
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy kk:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final String HTML_WRAPPER_TEMPLATE = "<!DOCTYPE html><html><head><style>body{font-size: 0.5cm;}</style><meta charset=\"%s\"><title>title</title></head><body>%s</body></html>";
    private static final String ADD_HEADER_IFRAME_JS_TAG_TEMPLATE = "<script id=\"header-v6a8oxpf48xfzy0rhjra\" data-file=\"%s\" type=\"text/javascript\">%s</script>";
    private static final String HEADER_FIELD_TEMPLATE = "<tr><td class=\"header-name\">%s</td><td class=\"header-value\">%s</td></tr>";

    private static final Pattern HTML_META_CHARSET_REGEX = Pattern.compile("(<meta(?!\\s*(?:name|value)\\s*=)[^>]*?charset\\s*=[\\s\"']*)([^\\s\"'/>]*)", Pattern.DOTALL);

    private static final Pattern IMG_CID_REGEX = Pattern.compile("cid:(.*?)\"", Pattern.DOTALL);
    private static final Pattern IMG_CID_PLAIN_REGEX = Pattern.compile("\\[cid:(.*?)]", Pattern.DOTALL);

    private static final String VIEWPORT_SIZE = "2480x3508";
    private static final int CONVERSION_DPI = 300;
    private static final int IMAGE_QUALITY = 100;
    public static final String HTML_CONTENT_TYPE = "text/html";
    public static final String TEXT_CONTENT_TYPE = "text/plain";
    public static final String HEADER_TEMPLATE_RESOURCE_PATH = "header.html";
    public static final String SCRIPT_TEMPLATE_RESOURCE_FILE = "contentScript.js";

    /*
     * Set System parameters to alleviate Java's built in Mime Parser strictness.
     */
    static {
        System.setProperty("mail.mime.address.strict", Boolean.FALSE.toString());
        System.setProperty("mail.mime.decodetext.strict", Boolean.FALSE.toString());
        System.setProperty("mail.mime.decodefilename", Boolean.TRUE.toString());
        System.setProperty("mail.mime.decodeparameters", Boolean.TRUE.toString());
        System.setProperty("mail.mime.multipart.ignoremissingendboundary", Boolean.TRUE.toString());
        System.setProperty("mail.mime.multipart.ignoremissingboundaryparameter", Boolean.TRUE.toString());

        System.setProperty("mail.mime.parameters.strict", Boolean.FALSE.toString());
        System.setProperty("mail.mime.applefilenames", Boolean.TRUE.toString());
        System.setProperty("mail.mime.ignoreunknownencoding", Boolean.TRUE.toString());
        System.setProperty("mail.mime.uudecode.ignoremissingbeginend", Boolean.TRUE.toString());
        System.setProperty("mail.mime.multipart.allowempty", Boolean.TRUE.toString());
        System.setProperty("mail.mime.multipart.ignoreexistingboundaryparameter", Boolean.TRUE.toString());

        System.setProperty("mail.mime.base64.ignoreerrors", Boolean.TRUE.toString());

        // set own cleaner class to handle broken contentTypes
        System.setProperty("mail.mime.contenttypehandler", "mimeparser.ContentTypeCleaner");
    }

    public StoredEmail storeEmail(MimeMessage message) throws MessagingException, IOException, InterruptedException {
        String subject = message.getSubject();

        if (StringUtils.isEmpty(subject)) {
            subject = message.getContentID();
        }

        LOGGER.info("Storing email '{}'.", message.getSubject());

        Path emailDirectory = Files.createTempDirectory("email-to-paperless-");
        Path messagePath = extractMessage(message, subject, emailDirectory);
        Path attachmentsDirectory = extractAttachments(message, emailDirectory);

        LOGGER.info("Email '{}' saved at '{}'.", subject, emailDirectory);

        return new StoredEmail(emailDirectory, messagePath, attachmentsDirectory);
    }

    private Path extractMessage(MimeMessage message, String subject, Path emailDirectory) throws MessagingException, IOException, InterruptedException {
        LOGGER.debug("Start conversion to html.");

        MimeBody bodyPart = findBodyPart(message);

        if (bodyPart == null) {
            LOGGER.info("Email does not contain a message.");
            return null;
        }

        Map<String, MimeInlineImage> inlineImages = parseInlineImages(message);
        String htmlBody = constructHTMLContent(bodyPart, inlineImages);

        Map.Entry<String, Path> htmlHeaderResult = applyHeaderToHTML(message, htmlBody);
        htmlBody = htmlHeaderResult.getKey();
        Path headerFile = htmlHeaderResult.getValue();
        LOGGER.debug("Temporary header file: {}", headerFile.toAbsolutePath());

        Path htmlFile = Files.createTempFile("email-", ".html");

        LOGGER.debug("Temporary html file: {}", htmlFile.toAbsolutePath());
        String charsetName = bodyPart.getContentType().getParameter("charset");
        Charset charset = Charset.forName(charsetName);
        Files.writeString(htmlFile, htmlBody, charset);

        LOGGER.debug("Successfully converted email to html.");

        LOGGER.debug("Start conversion of email to pdf.");

        Path outputPath = emailDirectory.resolve(parseToFileName(subject) + ".pdf");

        List<String> command = List.of(
                "wkhtmltopdf",
                "--viewport-size",
                VIEWPORT_SIZE,
                "--enable-local-file-access",
                "--dpi",
                String.valueOf(CONVERSION_DPI),
                "--image-quality",
                String.valueOf(IMAGE_QUALITY),
                "--encoding",
                charsetName,
                htmlFile.toAbsolutePath().toString(),
                outputPath.toAbsolutePath().toString()
        );

        executeCommand(command);

        LOGGER.debug("Cleaning up temporary files.");
        Files.delete(htmlFile);
        Files.delete(headerFile);

        return outputPath;
    }

    private Path extractAttachments(MimeMessage message, Path emailDirectory) throws IOException {
        Path attachmentsDirectory = emailDirectory.resolve("attachments");
        Files.createDirectories(attachmentsDirectory);

        LOGGER.info("Extract attachments to {}.", attachmentsDirectory.toAbsolutePath());

        List<AttachmentResource> attachments = EmailConverter.mimeMessageToEmail(message).getAttachments();

        LOGGER.debug("Found {} attachments.", attachments.size());
        for (AttachmentResource attachmentResource : attachments) {
            LOGGER.debug("Process Attachment {}", attachmentResource);

            String attachmentFilename = null;
            try {
                attachmentFilename = attachmentResource.getDataSource().getName();
            } catch (Exception ignored) {
                // ignore this error
            }

            Path attachmentPath;
            if (!StringUtils.isEmpty(attachmentFilename)) {
                attachmentPath = attachmentsDirectory.resolve(attachmentFilename);
            } else {
                String extension = "";

                // try to find at least the file extension via the mime type
                try {
                    extension = MimeTypes.getDefaultMimeTypes().forName(attachmentResource.getDataSource().getContentType()).getExtension();
                } catch (Exception ignored) {
                    // ignore this error
                }

                LOGGER.debug("Attachment {} did not hold any name, use random name", attachmentResource);
                attachmentPath = Files.createTempFile(attachmentsDirectory, "nameless-", extension);
            }

            try (OutputStream outputStream = Files.newOutputStream(attachmentPath)) {
                ByteStreams.copy(attachmentResource.getDataSourceInputStream(), outputStream);
            }

            LOGGER.debug("Saved Attachment {} to {}", attachmentResource, attachmentPath.toAbsolutePath());
        }

        LOGGER.info("Extracted {} attachments.", attachments.size());

        return attachmentsDirectory;
    }

    private static String constructHTMLContent(MimeBody body, Map<String, MimeInlineImage> inlineImages) {
        String content = body.getContent();
        String charsetName = body.getContentType().getParameter("charset");

        if (body.getContentType().match(HTML_CONTENT_TYPE)) {
            content = convertHTMLContentToHTML(content, charsetName, inlineImages);
        } else {
            content = convertTextContentToHTML(content, charsetName, inlineImages);
        }

        return content;
    }

    private static String convertHTMLContentToHTML(String content, String charsetName, Map<String, MimeInlineImage> inlineImages) {
        if (!inlineImages.isEmpty()) {
            LOGGER.debug("Embed the referenced images (cid) using <img src=\"data:image ...> syntax");

            // find embedded images and embed them in html using <img src="data:image ...> syntax
            content = StringReplacer.replace(content, IMG_CID_REGEX, matcher -> {
                MimeInlineImage image = inlineImages.get("<" + matcher.group(1) + ">");

                // found no image for this cid, just return the matches string as it is
                if (image == null) {
                    return matcher.group();
                }

                return String.format("data:%s;base64,%s", image.getContentType().getBaseType(), image.getBase64());
            });
        }

        // overwrite html declared charset with email header charset
        content = StringReplacer.replace(content, HTML_META_CHARSET_REGEX, matcher -> {
            String declaredCharset = matcher.group(2);

            if (!charsetName.equalsIgnoreCase(declaredCharset)) {
                LOGGER.debug("Html declared different charset ({}) then the email header ({}), override with email header", declaredCharset, charsetName);
            }

            return matcher.group(1) + charsetName;
        });
        return content;
    }

    private static String convertTextContentToHTML(String content, String charsetName, Map<String, MimeInlineImage> inlineImages) {
        LOGGER.debug("No html message body could be found, fall back to text/plain and embed it into a html document");

        content = "<div style=\"white-space: pre-wrap\">" + content.replace("\n", "<br>").replace("\r", "") + "</div>";
        content = String.format(HTML_WRAPPER_TEMPLATE, charsetName, content);

        if (!inlineImages.isEmpty()) {
            LOGGER.debug("Embed the referenced images (cid) using <img src=\"data:image ...> syntax");

            // find embedded images and embed them in html using <img src="data:image ...> syntax
            content = StringReplacer.replace(content, IMG_CID_PLAIN_REGEX, matcher -> {
                MimeInlineImage image = inlineImages.get("<" + matcher.group(1) + ">");

                // found no image for this cid, just return the matches string
                if (image == null) {
                    return matcher.group();
                }

                return String.format("<img src=\"data:%s;base64,%s\" />", image.getContentType().getBaseType(), image.getBase64());
            });
        }
        return content;
    }

    private MimeHeaders parseMimeHeaders(MimeMessage message) throws MessagingException {
        LOGGER.debug("Read and decode headers.");

        String subject = message.getSubject();

        String sender = message.getHeader("From", null);
        if (sender == null) {
            sender = message.getHeader("Sender", null);
        }

        try {
            // try to decode the sender, if it is encoded
            sender = MimeUtility.decodeText(MimeUtility.unfold(sender));
        } catch (Exception ignored) {
            // ignore this error
        }

        List<String> receivers = new ArrayList<>();
        String receiversRaw = message.getHeader("To", null);

        try {
            receiversRaw = MimeUtility.unfold(receiversRaw);
        } catch (Exception ignored) {
            // ignore this error
        }

        String[] split = receiversRaw.split(",");
        for (String receiver : split) {
            try {
                receivers.add(MimeUtility.decodeText(receiver));
            } catch (UnsupportedEncodingException exception) {
                LOGGER.error(exception);
            }
        }

        Instant sentDate = message.getSentDate().toInstant();

        MimeHeaders headers = new MimeHeaders(subject, sender, receivers, sentDate);
        LOGGER.trace("Parsed headers: {}", headers);
        return headers;
    }

    /**
     * Execute a command and redirect its output to the standard output.
     *
     * @param command list of the command and its parameters
     */
    private void executeCommand(List<String> command) throws InterruptedException {
        LOGGER.debug("Execute command: {}", () -> String.join(" ", command));

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException exception) {
            LOGGER.error(exception);
        }
    }

    private void walkMimeStructure(Part part, ExceptionBiConsumer<Part, Integer> callback) {
        walkMimeStructure(part, 0, callback);
    }

    @SneakyThrows
    private void walkMimeStructure(Part part, int level, ExceptionBiConsumer<Part, Integer> callback) {
        callback.accept(part, level);

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();

            for (int i = 0; i < multipart.getCount(); i++) {
                walkMimeStructure(multipart.getBodyPart(i), level + 1, callback);
            }
        }
    }

    private String getContentAsString(Part part) throws IOException, MessagingException {
        Object content;

        try {
            content = part.getContent();
        } catch (IOException | MessagingException exception) {
            // most likely the specified charset could not be found
            content = part.getInputStream();
        }

        String stringContent = null;

        if (content instanceof String contentString) {
            stringContent = contentString;
        } else if (content instanceof InputStream inputStream) {
            stringContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        return stringContent;
    }

    private MimeBody findBodyPart(MimeMessage message) {
        LOGGER.debug("Looking for the main body part.");

        AtomicReference<MimeBody> reference = new AtomicReference<>();

        walkMimeStructure(message, (currentPart, level) -> {
            // only process text/plain and text/html
            if (!currentPart.isMimeType(TEXT_CONTENT_TYPE) && !currentPart.isMimeType(HTML_CONTENT_TYPE)) {
                return;
            }

            String stringContent = getContentAsString(currentPart);
            boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(currentPart.getDisposition());

            if (StringUtils.isEmpty(stringContent) || isAttachment) {
                return;
            }

            // use text/plain entries only when we found nothing before
            if (reference.get() == null || currentPart.isMimeType(HTML_CONTENT_TYPE)) {
                reference.set(new MimeBody(stringContent, new ContentType(currentPart.getContentType())));
            }
        });

        MimeBody bodyPart = reference.get();
        LOGGER.trace("Found main body part: {}", bodyPart);
        return bodyPart;
    }

    private Map<String, MimeInlineImage> parseInlineImages(MimeMessage message) {
        LOGGER.debug("Extracting inline images.");

        Map<String, MimeInlineImage> images = new HashMap<>();

        walkMimeStructure(message, (currentPart, level) -> {
            if (!currentPart.isMimeType("image/*") || currentPart.getHeader("Content-Id") == null) {
                return;
            }

            String id = currentPart.getHeader("Content-Id")[0];

            String imageBase64;
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                currentPart.getDataHandler().writeTo(byteArrayOutputStream);
                imageBase64 = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
            }

            images.put(id, new MimeInlineImage(imageBase64, new ContentType(currentPart.getContentType())));
        });

        LOGGER.trace("Extracted inline images: {}", images);
        return images;
    }

    private Map.Entry<String, Path> applyHeaderToHTML(MimeMessage message, String htmlBody) throws IOException, MessagingException {
        Path headerFile = Files.createTempFile("email-header-", ".html");

        String headerTemplate = Resources.toString(Resources.getResource(HEADER_TEMPLATE_RESOURCE_PATH), StandardCharsets.UTF_8);
        StringBuilder headerBuilder = new StringBuilder();

        MimeHeaders headers = parseMimeHeaders(message);

        String sender = headers.getSender();
        if (!StringUtils.isEmpty(sender)) {
            headerBuilder
                    .append(String.format(HEADER_FIELD_TEMPLATE, "Von", HtmlEscapers.htmlEscaper().escape(sender)))
                    .append(System.lineSeparator());
        }

        List<String> receivers = headers.getReceivers();
        if (!receivers.isEmpty()) {
            headerBuilder
                    .append(String.format(HEADER_FIELD_TEMPLATE, "Empfänger", HtmlEscapers.htmlEscaper().escape(String.join(", ", receivers))))
                    .append(System.lineSeparator());
        }

        String subject = message.getSubject();
        if (!StringUtils.isEmpty(subject)) {
            headerBuilder
                    .append(String.format(HEADER_FIELD_TEMPLATE, "Betreff", String.format("<b>%s<b>", HtmlEscapers.htmlEscaper().escape(subject))))
                    .append(System.lineSeparator());
        }

        Instant sentDate = headers.getSentDate();
        if (sentDate != null) {
            headerBuilder
                    .append(String.format(HEADER_FIELD_TEMPLATE, "Datum", HtmlEscapers.htmlEscaper().escape(DATE_TIME_FORMATTER.format(sentDate))))
                    .append(System.lineSeparator());
        }

        Files.writeString(headerFile, String.format(headerTemplate, headerBuilder), StandardCharsets.UTF_8);

        // Append this script tag dirty to the bottom
        htmlBody += String.format(ADD_HEADER_IFRAME_JS_TAG_TEMPLATE, headerFile.toUri(),
                Resources.toString(Resources.getResource(SCRIPT_TEMPLATE_RESOURCE_FILE), StandardCharsets.UTF_8));

        return new AbstractMap.SimpleEntry<>(htmlBody, headerFile);
    }

    private String parseToFileName(String text) {
        String fileName = text.trim().replaceAll("[^a-z-_0-9A-ZäüöÄÜÖ\\s]", "_");

        if (fileName.length() > 200) {
            fileName = fileName.substring(0, 200);
        }

        return fileName;
    }

}