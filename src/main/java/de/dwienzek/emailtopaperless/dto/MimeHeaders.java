package de.dwienzek.emailtopaperless.dto;

import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
public class MimeHeaders {

    String subject;
    String sender;
    List<String> receivers;
    Instant sentDate;

}
