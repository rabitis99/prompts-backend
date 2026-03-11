package org.example.sharedprompts.domain.prompt.common.i18n;

import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;

/**
 * Core i18n text triple (ko, en, ja) used across enums and guideline metadata.
 */
public record I18nText(String ko, String en, String ja) {

    public static I18nText of(String ko, String en, String ja) {
        return new I18nText(ko, en, ja);
    }

    /**
     * Returns the text for the given language; defaults to Korean when lang is null.
     */
    public String byLang(LanguageType lang) {
        if (lang == null) {
            return ko;
        }
        return switch (lang) {
            case KOREAN -> ko;
            case ENGLISH -> (en == null || en.isBlank()) ? ko : en;
            case JAPANESE -> (ja == null || ja.isBlank()) ? ko : ja;
        };
    }
}

