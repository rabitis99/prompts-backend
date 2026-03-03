package org.example.sharedprompts.domain.prompt.common.enums;

import java.util.Objects;
import java.util.function.Function;

/**
 * 다국어 텍스트 처리를 위한 공통 유틸리티 클래스
 * <p>LanguageType에 따라 적절한 텍스트를 반환하는 공통 로직을 제공합니다.</p>
 */
public final class I18nUtils {

    private I18nUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 언어 타입에 따라 적절한 텍스트를 반환한다.
     * <p>함수형 인터페이스를 사용하여 각 enum의 필드에 접근합니다.</p>
     *
     * @param lang 언어 타입 (null이면 안 됨)
     * @param textProvider 언어 타입에 따라 적절한 텍스트를 제공하는 함수
     * @return 해당 언어의 텍스트
     * @throws NullPointerException lang이 null인 경우
     */
    public static String getByLang(LanguageType lang, Function<LanguageType, String> textProvider) {
        Objects.requireNonNull(lang, "lang must not be null");
        Objects.requireNonNull(textProvider, "textProvider must not be null");
        return textProvider.apply(lang);
    }

    /**
     * 언어 타입에 따라 적절한 텍스트를 반환한다.
     * <p>3개의 언어별 텍스트를 직접 받아서 처리합니다.</p>
     *
     * @param lang 언어 타입 (null이면 안 됨)
     * @param textKo 한국어 텍스트
     * @param textEn 영어 텍스트
     * @param textJa 일본어 텍스트
     * @return 해당 언어의 텍스트
     * @throws NullPointerException lang이 null인 경우
     */
    public static String getByLang(LanguageType lang, String textKo, String textEn, String textJa) {
        Objects.requireNonNull(lang, "lang must not be null");
        return switch (lang) {
            case KOREAN -> textKo;
            case ENGLISH -> textEn;
            case JAPANESE -> textJa;
        };
    }
}

