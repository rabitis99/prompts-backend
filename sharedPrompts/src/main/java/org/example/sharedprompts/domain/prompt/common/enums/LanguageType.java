package org.example.sharedprompts.domain.prompt.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 응답 생성 시 사용할 언어.
 */
@Getter
@AllArgsConstructor
public enum LanguageType {
    /** 한국어 */
    KOREAN("한국어", "Korean"),

    /** 영어 */
    ENGLISH("영어", "English"),

    /** 일본어 */
    JAPANESE("일본어", "Japanese");

    private final String description;
    private final String promptToken;
}
