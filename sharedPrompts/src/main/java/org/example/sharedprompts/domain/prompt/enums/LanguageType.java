package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LanguageType {
    KOREAN("한국어"),
    ENGLISH("영어"),
    JAPANESE("일본어");

    private final String description;
}
