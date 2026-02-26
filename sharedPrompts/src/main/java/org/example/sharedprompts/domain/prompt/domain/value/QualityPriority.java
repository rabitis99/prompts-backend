package org.example.sharedprompts.domain.prompt.domain.value;

/**
 * 품질 우선순위 — Rubric 항목의 가중치 순서를 결정한다.
 * UX에는 노출하지 않으며, 내부 엔진이 자동 매핑한다.
 */
public enum QualityPriority {

    ACCURACY_FIRST("정확도 최우선 — 사실·수치·인용 검증을 먼저 수행한다"),
    STRUCTURE_FIRST("구조 최우선 — 필수 섹션 커버리지와 형식 준수를 먼저 수행한다"),
    BREVITY_SECOND("간결성 보조 — 기본 정확도 달성 후 길이·중복 압축을 수행한다"),
    CREATIVITY_SECOND("창의성 보조 — 기본 제약 준수 후 창의적 다양성을 허용한다");

    private final String description;

    QualityPriority(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
