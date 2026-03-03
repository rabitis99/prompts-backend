package org.example.sharedprompts.domain.prompt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LanguageType {
    KOREAN("한국어", "Korean"),
    ENGLISH("영어", "English"),
    JAPANESE("일본어", "Japanese");

    private final String description;
    private final String promptToken;
}
