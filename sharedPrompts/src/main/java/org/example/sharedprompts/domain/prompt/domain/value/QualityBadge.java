package org.example.sharedprompts.domain.prompt.domain.value;

/**
 * 품질 배지 — UX에 노출되는 유일한 품질 지표.
 * pass rate/repair count 등 수치는 응답에 포함하지 않는다.
 *
 * <p>배지 변환 로직은 {@code BadgeResponseAssembler}(adapter/in/web 전용)에서 수행하며,
 * domain/application 레이어를 침범하지 않는다.
 */
public enum QualityBadge {
    /** Coverage 검사 통과 — 항상 포함 */
    CONDITIONS_MET("조건 반영됨"),

    /** Schema/OutputContract 통과 — 항상 포함 */
    FORMAT_VERIFIED("형식 검증 완료"),

    /** ContentSandbox 통과 — 항상 포함 */
    NO_PROHIBITED_CONTENT("금지어 없음"),

    /** Repair 0회, First-pass 통과 — 선택적 */
    FAST_GENERATION("빠른 생성"),

    /** Repair 1~2회 후 통과 — 선택적 (횟수는 숨김) */
    REVERIFIED("재검증 완료");

    private final String displayName;

    QualityBadge(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
