package org.example.sharedprompts.domain.prompt.domain.model;

import java.util.List;

/**
 * 콘텐츠 안전 경계 — 금지어·정책 위반 패턴을 정의한다.
 * Verify(SOFT 모드 포함) 단계에서 NO_PROHIBITED_CONTENT 루브릭 항목 검증에 사용된다.
 */
public final class ContentSandbox {

    private static final List<String> DEFAULT_PROHIBITED_PATTERNS = List.of(
        "개인정보", "주민등록번호", "비밀번호", "카드번호"
    );

    private final List<String> prohibitedPatterns;
    private final boolean strictMode;

    private ContentSandbox(List<String> prohibitedPatterns, boolean strictMode) {
        this.prohibitedPatterns = prohibitedPatterns;
        this.strictMode = strictMode;
    }

    public static ContentSandbox defaults() {
        return new ContentSandbox(DEFAULT_PROHIBITED_PATTERNS, false);
    }

    public static ContentSandbox strict(List<String> additionalPatterns) {
        List<String> combined = new java.util.ArrayList<>(DEFAULT_PROHIBITED_PATTERNS);
        if (additionalPatterns != null) {
            combined.addAll(additionalPatterns);
        }
        return new ContentSandbox(List.copyOf(combined), true);
    }

    public List<String> getProhibitedPatterns() { return prohibitedPatterns; }
    public boolean isStrictMode() { return strictMode; }

    public boolean containsViolation(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return prohibitedPatterns.stream().anyMatch(p -> lower.contains(p.toLowerCase()));
    }
}
