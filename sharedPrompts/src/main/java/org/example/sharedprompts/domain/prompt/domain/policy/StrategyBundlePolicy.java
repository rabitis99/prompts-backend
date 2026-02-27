package org.example.sharedprompts.domain.prompt.domain.policy;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 전략 번들 정책 — Objective당 허용 번들과 호출 상한 가드레일을 관리한다.
 *
 * <p><b>가드레일 (구조로 강제):</b>
 * <ul>
 *   <li>Objective당 StrategyBundle 최대 3개 (초과 시 즉시 예외)</li>
 *   <li>Experimental 전략 동시 1개 (2개 이상 요청 시 우선순위 낮은 것 자동 비활성화 + 경고)</li>
 *   <li>Objective별 LLM 호출 상한 초과 시 Objective-specific/Experimental 전략 비활성화 → Core만 적용</li>
 * </ul>
 */
@Slf4j
@Component
public class StrategyBundlePolicy {

    /** Objective → 기본 전략 번들 매핑 */
    private static final Map<PromptObjective, PromptStrategyBundle> DEFAULT_BUNDLES;

    static {
        DEFAULT_BUNDLES = new EnumMap<>(PromptObjective.class);

        DEFAULT_BUNDLES.put(PromptObjective.FACTUAL,
            PromptStrategyBundle.of("FACTUAL_DEFAULT", EnumSet.of(
                PromptingStrategy.CLARIFY_FIRST,
                PromptingStrategy.STEP_BY_STEP,
                PromptingStrategy.CHECKLIST_VERIFY,
                PromptingStrategy.CHAIN_OF_VERIFICATION,
                PromptingStrategy.CITE_OR_UNCERTAIN
            ))
        );

        DEFAULT_BUNDLES.put(PromptObjective.REASONING,
            PromptStrategyBundle.of("REASONING_DEFAULT", EnumSet.of(
                PromptingStrategy.CLARIFY_FIRST,
                PromptingStrategy.STEP_BY_STEP,
                PromptingStrategy.CHECKLIST_VERIFY,
                PromptingStrategy.REQUIRE_JUSTIFICATION
            ))
        );

        DEFAULT_BUNDLES.put(PromptObjective.EXTRACTION,
            PromptStrategyBundle.of("EXTRACTION_DEFAULT", EnumSet.of(
                PromptingStrategy.CLARIFY_FIRST,
                PromptingStrategy.STEP_BY_STEP,
                PromptingStrategy.CHECKLIST_VERIFY
            ))
        );

        DEFAULT_BUNDLES.put(PromptObjective.PLANNING,
            PromptStrategyBundle.of("PLANNING_DEFAULT", EnumSet.of(
                PromptingStrategy.CLARIFY_FIRST,
                PromptingStrategy.STEP_BY_STEP,
                PromptingStrategy.CHECKLIST_VERIFY,
                PromptingStrategy.DECOMPOSITION,
                PromptingStrategy.EDGE_CASE_SCAN
            ))
        );

        DEFAULT_BUNDLES.put(PromptObjective.CREATIVE_WITH_CONSTRAINTS,
            PromptStrategyBundle.of("CREATIVE_DEFAULT", EnumSet.of(
                PromptingStrategy.CLARIFY_FIRST,
                PromptingStrategy.STEP_BY_STEP,
                PromptingStrategy.CHECKLIST_VERIFY,
                PromptingStrategy.FEW_SHOT_EXEMPLAR
            ))
        );

        DEFAULT_BUNDLES.put(PromptObjective.ANALYTICAL,
            PromptStrategyBundle.of("ANALYTICAL_DEFAULT", EnumSet.of(
                PromptingStrategy.CLARIFY_FIRST,
                PromptingStrategy.STEP_BY_STEP,
                PromptingStrategy.CHECKLIST_VERIFY,
                PromptingStrategy.CHAIN_OF_VERIFICATION,
                PromptingStrategy.CITE_OR_UNCERTAIN
            ))
        );
    }

    /**
     * Objective에 대한 기본 전략 번들을 반환한다.
     * LLM 호출 상한 초과 시 Core 번들로 자동 다운그레이드한다.
     */
    public PromptStrategyBundle resolveBundle(PromptObjective objective, boolean experimentalEnabled) {
        PromptStrategyBundle bundle = DEFAULT_BUNDLES.getOrDefault(objective, PromptStrategyBundle.coreOnly());

        // Experimental 전략 처리: 플래그 없으면 비활성화
        if (!experimentalEnabled) {
            bundle = removeExperimentalStrategies(bundle, objective);
        }

        // 호출 상한 검증: 초과 시 Core만 적용
        int totalCalls = 1 + bundle.totalAdditionalLlmCalls(); // Solve 호출 1회 포함
        if (totalCalls > objective.getMaxLlmCallCount()) {
            log.warn("[StrategyBundlePolicy] LLM 호출 상한 초과 → Core 전략만 적용: objective={}, totalCalls={}, maxCalls={}",
                    objective, totalCalls, objective.getMaxLlmCallCount());
            return PromptStrategyBundle.coreOnly();
        }

        return bundle;
    }

    private PromptStrategyBundle removeExperimentalStrategies(PromptStrategyBundle bundle, PromptObjective objective) {
        List<PromptingStrategy> experimental = bundle.getExperimentalStrategies();
        if (experimental.isEmpty()) return bundle;

        log.debug("[StrategyBundlePolicy] Experimental 전략 비활성화 (플래그 미설정): objective={}, removed={}",
                objective, experimental);

        Set<PromptingStrategy> filtered = new LinkedHashSet<>(bundle.getStrategies());
        filtered.removeAll(experimental);

        return PromptStrategyBundle.of(bundle.getName() + "_NO_EXP",
                filtered.isEmpty() ? EnumSet.of(PromptingStrategy.CLARIFY_FIRST) : EnumSet.copyOf(filtered));
    }

}
