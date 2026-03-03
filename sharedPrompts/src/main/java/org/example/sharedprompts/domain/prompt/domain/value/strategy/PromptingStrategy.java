package org.example.sharedprompts.domain.prompt.domain.value.strategy;

import lombok.Getter;

/**
 * 프롬프트 전략 목록 — 3계층(Core / Objective-specific / Experimental)으로 분류된다.
 *
 * <p><b>가드레일:</b>
 * <ul>
 *   <li>Core 전략은 항상 활성화된다.</li>
 *   <li>Objective-specific 전략은 Objective에 따라 조건부 활성화된다.</li>
 *   <li>Experimental 전략은 experimentalEnabled 플래그가 true일 때만 활성화되며,
 *       동시에 1개만 허용된다.</li>
 * </ul>
 */
@Getter
public enum PromptingStrategy {

    // ─────────── Core (항상 적용, LLM 추가 호출 0) ───────────
    CLARIFY_FIRST(Tier.CORE, 0),
    STEP_BY_STEP(Tier.CORE, 0),
    CHECKLIST_VERIFY(Tier.CORE, 0),

    // ─────────── Objective-specific (조건부, LLM 추가 호출 1~2) ───────────
    DECOMPOSITION(Tier.OBJECTIVE_SPECIFIC, 1),
    CITE_OR_UNCERTAIN(Tier.OBJECTIVE_SPECIFIC, 1),
    REQUIRE_JUSTIFICATION(Tier.OBJECTIVE_SPECIFIC, 1),
    FEW_SHOT_EXEMPLAR(Tier.OBJECTIVE_SPECIFIC, 1),
    CHAIN_OF_VERIFICATION(Tier.OBJECTIVE_SPECIFIC, 2),
    EDGE_CASE_SCAN(Tier.OBJECTIVE_SPECIFIC, 1),

    // ─────────── Experimental (A/B 실험 플래그로만, LLM 추가 호출 2~4) ───────────
    SELF_CONSISTENCY(Tier.EXPERIMENTAL, 4),
    TREE_OF_THOUGHTS(Tier.EXPERIMENTAL, 4);

    private final Tier tier;
    private final int additionalLlmCalls;

    PromptingStrategy(Tier tier, int additionalLlmCalls) {
        this.tier = tier;
        this.additionalLlmCalls = additionalLlmCalls;
    }

    public boolean isCore() {
        return tier == Tier.CORE;
    }

    public boolean isExperimental() {
        return tier == Tier.EXPERIMENTAL;
    }

    public enum Tier {
        CORE,
        OBJECTIVE_SPECIFIC,
        EXPERIMENTAL
    }
}
