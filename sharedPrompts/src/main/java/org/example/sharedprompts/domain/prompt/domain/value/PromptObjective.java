package org.example.sharedprompts.domain.prompt.domain.value;

/**
 * 프롬프트 생성 목적(Objective) — Verify 강도와 LLM 호출 상한을 결정한다.
 *
 * <ul>
 *   <li>FACTUAL: 사실 검증 중심, CoV 강화, 호출 상한 3</li>
 *   <li>REASONING: 단계별 추론, 근거 요구, 호출 상한 2</li>
 *   <li>EXTRACTION: JSON Schema 우선, Constrained Decoding, 호출 상한 1</li>
 *   <li>PLANNING: 분해/엣지케이스, 호출 상한 2</li>
 *   <li>CREATIVE_WITH_CONSTRAINTS: Soft-verify(형식·금지어·명백한 모순만), 호출 상한 2</li>
 * </ul>
 */
public enum PromptObjective {

    FACTUAL(3, VerifyMode.CHAIN_OF_VERIFICATION),
    REASONING(2, VerifyMode.STANDARD),
    EXTRACTION(1, VerifyMode.SCHEMA_FIRST),
    PLANNING(2, VerifyMode.STANDARD),
    CREATIVE_WITH_CONSTRAINTS(2, VerifyMode.SOFT);

    private final int maxLlmCallCount;
    private final VerifyMode verifyMode;

    PromptObjective(int maxLlmCallCount, VerifyMode verifyMode) {
        this.maxLlmCallCount = maxLlmCallCount;
        this.verifyMode = verifyMode;
    }

    public int getMaxLlmCallCount() {
        return maxLlmCallCount;
    }

    public VerifyMode getVerifyMode() {
        return verifyMode;
    }

    public enum VerifyMode {
        /** Chain-of-Verification 전체 실행, 사실/수치/인용 검증 우선 */
        CHAIN_OF_VERIFICATION,
        /** 루브릭 Coverage + 모순 탐지 */
        STANDARD,
        /** JSON Schema 기반 검증 우선, 통과 시 LLM Verify 생략 가능 */
        SCHEMA_FIRST,
        /** 형식·톤 준수 + 명백한 모순 여부만 체크 */
        SOFT
    }
}
