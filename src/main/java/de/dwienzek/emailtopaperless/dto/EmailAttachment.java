package de.dwienzek.emailtopaperless.dto;

import lombok.Data;

@Data
public class EmailAttachment {

    private final String fileName;
    private final String contentType;

}
