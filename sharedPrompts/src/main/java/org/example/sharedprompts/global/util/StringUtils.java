package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 문자열 처리 유틸리티 클래스
 * 
 * 공통으로 사용되는 문자열 처리 로직을 제공합니다.
 * 모든 메서드는 static이므로 인스턴스화를 방지합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtils {

    /**
     * 문자열을 trim하고, null이거나 빈 문자열이면 null을 반환합니다.
     * 
     * @param str 처리할 문자열
     * @return trim된 문자열 또는 null
     */
    public static String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 문자열을 trim하고, null이거나 빈 문자열이면 빈 문자열을 반환합니다.
     * 
     * @param str 처리할 문자열
     * @return trim된 문자열 또는 빈 문자열
     */
    public static String trimToEmpty(String str) {
        if (str == null) {
            return "";
        }
        return str.trim();
    }
}

