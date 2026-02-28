package org.example.sharedprompts.domain.prompt.domain.value;

/**
 * 프롬프트 생성 목적(Objective) — 값(value) 식별자 역할만 담당한다.
 *
 * <p>행위(maxLlmCallCount, verifyMode 등)는 {@link org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveProfile}
 * 구현체로 이동했다. {@code ObjectiveRegistry.get(this)}로 프로파일을 조회하라.
 *
 * <p>DB 컬럼값 = {@code name()} (기존 데이터 마이그레이션 불필요).
 */
public enum PromptObjective {

    FACTUAL,
    REASONING,
    EXTRACTION,
    PLANNING,
    CREATIVE_WITH_CONSTRAINTS,
    ANALYTICAL
}
