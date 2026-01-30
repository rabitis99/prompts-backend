package org.example.sharedprompts.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 민감 정보 마스킹 유틸리티
 *
 * 개인정보 보호를 위해 로그에 기록되는 민감 정보를 마스킹합니다.
 * 이메일, 토큰 등 다양한 타입의 민감 정보를 일관된 방식으로 마스킹합니다.
 * 모든 메서드는 static이므로 인스턴스화를 방지합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SensitiveDataMasker {

    private static final String MASK = "***";
    private static final int EMAIL_VISIBLE_PREFIX_LENGTH = 2;
    private static final int TOKEN_VISIBLE_PREFIX_LENGTH = 4;
    private static final int TOKEN_VISIBLE_SUFFIX_LENGTH = 4;
    private static final int MIN_TOKEN_LENGTH = 8;
    private static final double MIN_MASKING_RATIO = 0.5; // 최소 50% 마스킹 보장

    /**
     * 이메일 주소를 마스킹합니다.
     *
     * <p>마스킹 규칙:
     * - @ 이전 부분: 앞 2자만 표시하고 나머지는 ***로 마스킹
     * - @ 이후 부분(도메인): 그대로 표시
     * - 이메일이 너무 짧거나 형식이 잘못된 경우: 전체 마스킹
     *
     * @param email 원본 이메일 주소
     * @return 마스킹된 이메일 주소 (null이면 "***" 반환)
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return MASK;
        }

        int atIndex = email.indexOf('@');

        // @가 없거나 위치가 잘못된 경우 전체 마스킹
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            return MASK;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        // 로컬 파트가 너무 짧으면 전체 마스킹
        if (localPart.length() <= EMAIL_VISIBLE_PREFIX_LENGTH) {
            return MASK + domain;
        }

        // 앞 2자만 표시하고 나머지는 마스킹
        String visiblePrefix = localPart.substring(0, EMAIL_VISIBLE_PREFIX_LENGTH);
        return visiblePrefix + MASK + domain;
    }

    /**
     * 토큰 값을 마스킹합니다.
     *
     * <p>마스킹 규칙:
     * - 앞 4자리와 뒤 4자리만 표시하고 나머지는 마스킹 (기본)
     * - 최소 50% 이상 마스킹 보장
     * - 토큰이 너무 짧으면 전체 마스킹
     *
     * @param token 원본 토큰
     * @return 마스킹된 토큰 (null이면 "***" 반환)
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= MIN_TOKEN_LENGTH) {
            return MASK;
        }

        int tokenLength = token.length();
        int maxVisibleLength = (int) (tokenLength * (1 - MIN_MASKING_RATIO));

        // 최소 마스킹 비율을 보장하기 위해 표시할 수 있는 최대 길이 계산
        int visiblePrefixLength = Math.min(TOKEN_VISIBLE_PREFIX_LENGTH, maxVisibleLength / 2);
        int visibleSuffixLength = Math.min(TOKEN_VISIBLE_SUFFIX_LENGTH, maxVisibleLength / 2);

        // 표시할 부분의 총 길이가 토큰 길이보다 크거나 같으면 전체 마스킹
        if (visiblePrefixLength + visibleSuffixLength >= tokenLength) {
            return MASK;
        }

        // 앞 부분과 뒤 부분만 표시, 나머지는 마스킹
        return token.substring(0, visiblePrefixLength) +
                "..." +
                token.substring(tokenLength - visibleSuffixLength);
    }

    /**
     * 일반 문자열을 마스킹합니다.
     *
     * <p>마스킹 규칙:
     * - 앞 2자리만 표시하고 나머지는 마스킹
     * - 문자열이 너무 짧으면 전체 마스킹
     *
     * @param value 원본 문자열
     * @return 마스킹된 문자열 (null이면 "***" 반환)
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return MASK;
        }

        if (value.length() <= 2) {
            return MASK;
        }

        return value.substring(0, 2) + MASK;
    }
}
