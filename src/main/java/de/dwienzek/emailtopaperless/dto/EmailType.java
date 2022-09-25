package de.dwienzek.emailtopaperless.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public enum EmailType {

    HTML(".html"),
    TEXT(".rtf"),
    FILE(null);

    private final String fileEnding;

}
