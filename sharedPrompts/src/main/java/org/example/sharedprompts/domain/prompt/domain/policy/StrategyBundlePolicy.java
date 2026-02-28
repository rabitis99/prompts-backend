package org.example.sharedprompts.domain.prompt.domain.policy;

import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveProfile;
import org.example.sharedprompts.domain.prompt.domain.objective.ObjectiveRegistry;
import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.value.PromptStrategyBundle;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 전략 번들 정책 — Objective당 기본 번들과 호출 상한 가드레일을 관리한다.
 *
 * <p>Spring 의존 없음 — {@code PromptDomainConfig}에서 생성·주입한다.
 *
 * <p><b>가드레일:</b>
 * <ul>
 *   <li>Experimental 전략 동시 1개 (PromptStrategyBundle 생성자에서 강제)</li>
 *   <li>Objective별 LLM 호출 상한 초과 시 Core 전략만 적용</li>
 * </ul>
 *
 * <p>기본 번들은 각 {@link ObjectiveProfile#defaultBundle()}에서 가져온다.
 * switch/case 없음.
 */
public class StrategyBundlePolicy {

    private final ObjectiveRegistry objectiveRegistry;

    public StrategyBundlePolicy(ObjectiveRegistry objectiveRegistry) {
        this.objectiveRegistry = objectiveRegistry;
    }

    /**
     * Objective에 대한 전략 번들을 결정한다.
     * LLM 호출 상한 초과 시 Core 번들로 자동 다운그레이드.
     */
    public PromptStrategyBundle resolveBundle(PromptObjective objective, boolean experimentalEnabled) {
        ObjectiveProfile profile = objectiveRegistry.get(objective);
        PromptStrategyBundle bundle = profile.defaultBundle();

        if (!experimentalEnabled) {
            bundle = removeExperimentalStrategies(bundle, objective);
        }

        int totalCalls = 1 + bundle.totalAdditionalLlmCalls(); // Solve 1회 포함
        if (totalCalls > profile.maxLlmCallCount()) {
            return PromptStrategyBundle.coreOnly();
        }

        return bundle;
    }

    private PromptStrategyBundle removeExperimentalStrategies(PromptStrategyBundle bundle, PromptObjective objective) {
        List<PromptingStrategy> experimental = bundle.getExperimentalStrategies();
        if (experimental.isEmpty()) return bundle;

        Set<PromptingStrategy> filtered = new LinkedHashSet<>(bundle.getStrategies());
        filtered.removeAll(experimental);

        if (filtered.isEmpty()) {
            return PromptStrategyBundle.coreOnly();
        }
        return PromptStrategyBundle.of(
                bundle.getName() + "_NO_EXP",
                EnumSet.copyOf(filtered)
        );
    }
}
