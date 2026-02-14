package org.example.sharedprompts.domain.prompt.guideline;

import org.example.sharedprompts.domain.prompt.enums.LanguageType;

/**
 * 다국어 텍스트 record (Ko, En, Ja)
 */
public record I18nText(String ko, String en, String ja) {

    public static I18nText of(String ko, String en, String ja) {
        return new I18nText(ko, en, ja);
    }

    /**
     * 언어 타입에 따라 해당 언어의 텍스트를 반환합니다.
     *
     * @param lang 언어 타입 (KOREAN, ENGLISH, JAPANESE)
     *             null인 경우 한국어(ko)를 기본값으로 반환합니다.
     * @return 해당 언어의 텍스트. lang이 null이면 한국어 텍스트를 반환합니다.
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


