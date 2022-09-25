package de.dwienzek.emailtopaperless.dto;

import lombok.Value;

import javax.mail.internet.ContentType;

@Value
public class MimeBody {

    String content;
    ContentType contentType;

}
