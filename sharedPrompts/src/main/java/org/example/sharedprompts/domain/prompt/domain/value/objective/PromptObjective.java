package org.example.sharedprompts.domain.prompt.domain.value.objective;

/**
 * 프롬프트 생성 목적(Objective) — 도메인 값 객체.
 *
 * <p>행위(maxLlmCallCount, verifyMode 등)는 {@link org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveProfile}
 * 구현체로 이동했다. {@code ObjectiveRegistry.get(this)}로 프로파일을 조회하라.
 *
 * <p>DB 컬럼값 = {@code name()} (기존 데이터 마이그레이션 불필요).
 * API·라우팅 계약용 목적은 {@link org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective}를 사용하며,
 * 문서에서는 "Domain Prompt Objective"로 구분한다. 자세한 내용은 {@code docs/PROMPT_COMMON.md} "2.0 PromptObjective 이중 정의" 참고.</p>
 */
public enum PromptObjective {

    /** 사실 전달·요약·번역 */
    FACTUAL,

    /** 설명·분석·디버깅 등 논리 추론 */
    REASONING,

    /** 추출·분류·구조화 */
    EXTRACTION,

    /** 계획·로드맵·체크리스트 */
    PLANNING,

    /** 창작·작문 (제약 하 창의) */
    CREATIVE_WITH_CONSTRAINTS,

    /** 비교·평가·원인 분석 등 분석형 */
    ANALYTICAL
}
