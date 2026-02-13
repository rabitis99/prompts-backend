package org.example.sharedprompts.domain.prompt.enums.guideline;

import org.example.sharedprompts.domain.prompt.enums.LanguageType;

/**
 * 다국어 텍스트 record (Ko, En, Ja)
 */
public record I18nText(String ko, String en, String ja) {

    public static I18nText of(String ko, String en, String ja) {
        return new I18nText(ko, en, ja);
    }

    public String byLang(LanguageType lang) {
        if (lang == null) {
            return ko;
        }
        return switch (lang) {
            case KOREAN -> ko;
            case ENGLISH -> en;
            case JAPANESE -> ja;
        };
    }
}
