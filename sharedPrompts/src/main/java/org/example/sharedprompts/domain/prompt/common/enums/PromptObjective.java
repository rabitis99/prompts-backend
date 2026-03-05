package org.example.sharedprompts.domain.prompt.common.enums;

/**
 * 상위 레벨 프롬프트 목적 (API/메타데이터용).
 *
 * <p>Unified 엔드포인트·라우팅·응답에서 사용한다.
 * 도메인 파이프라인용 목적은 {@link org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective}를 사용하며,
 * CREATIVE → CREATIVE_WITH_CONSTRAINTS, CODE → 도메인 기본(REASONING 등)으로 매핑된다.</p>
 */
public enum PromptObjective {

    /** 사실 전달·요약·번역 등 */
    FACTUAL,

    /** 설명·분석·평가·디버깅 등 논리 추론 */
    REASONING,

    /** 계획·로드맵·체크리스트 등 */
    PLANNING,

    /** 창작·생성·작문 (도메인: CREATIVE_WITH_CONSTRAINTS) */
    CREATIVE,

    /** 추출·분류·구조화 (JSON 등) */
    EXTRACTION,

    /** 코드 생성·리뷰 등 기술 출력 */
    CODE;

    /**
     * 도메인 계층 Objective로 변환.
     * API 응답에는 이 enum을 그대로 쓰고, 스펙/검증에는 도메인 enum을 사용할 때 호출한다.
     */
    public org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective toDomainObjective() {
        return switch (this) {
            case FACTUAL -> org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective.FACTUAL;
            case REASONING -> org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective.REASONING;
            case PLANNING -> org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective.PLANNING;
            case CREATIVE -> org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective.CREATIVE_WITH_CONSTRAINTS;
            case EXTRACTION -> org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective.EXTRACTION;
            case CODE -> org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective.REASONING;
        };
    }
}

