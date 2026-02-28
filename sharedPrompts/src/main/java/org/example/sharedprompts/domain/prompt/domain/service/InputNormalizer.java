package org.example.sharedprompts.domain.prompt.domain.service;

/**
 * 프롬프트 raw 입력 정규화 (도메인 순수, Spring 무의존).
 *
 * <p>과도한 공백·특수문자 제거 등 Clarify 단계에서 한 곳에서만 사용.
 * Sanitization 등 다른 레이어에서 동일 규칙이 필요하면 이 클래스를 재사용한다.
 */
public final class InputNormalizer {

    private InputNormalizer() {}

    /**
     * null이면 빈 문자열 반환. trim 후 연속 공백(3개 이상)을 2칸으로 축소.
     */
    public static String normalize(String input) {
        if (input == null) return "";
        return input.strip().replaceAll("\\s{3,}", "  ");
    }
}
