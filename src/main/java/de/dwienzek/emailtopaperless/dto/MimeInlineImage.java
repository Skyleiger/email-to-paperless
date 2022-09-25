package de.dwienzek.emailtopaperless.dto;

import lombok.Value;

import jakarta.mail.internet.ContentType;

@Value
public class MimeInlineImage {

    String base64;
    ContentType contentType;

}
