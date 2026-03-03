package org.example.sharedprompts.domain.prompt.common.guideline.i18n;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;

/**
 * 다국어 텍스트 record (Ko, En, Ja)
 */
public record I18nText(String ko, String en, String ja) {

    public static I18nText of(String ko, String en, String ja) {
        return new I18nText(ko, en, ja);
    }

    /**
     * 언어 타입에 따라 해당 언어의 텍스트를 반환합니다.
     */
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
