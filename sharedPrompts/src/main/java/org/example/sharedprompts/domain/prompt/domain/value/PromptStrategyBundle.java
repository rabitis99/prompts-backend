package org.example.sharedprompts.domain.prompt.domain.value;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Objective에 적용할 전략 묶음.
 *
 * <p><b>가드레일 (구조로 강제):</b>
 * <ul>
 *   <li>Experimental 전략은 최대 1개만 포함할 수 있다.</li>
 *   <li>Bundle 내 총 LLM 추가 호출 수는 Objective 상한을 초과할 수 없다
 *       (상한 검증은 {@link org.example.sharedprompts.domain.prompt.domain.policy.StrategyBundlePolicy}에서 수행).</li>
 * </ul>
 */
public final class PromptStrategyBundle {

    private final String name;
    private final Set<PromptingStrategy> strategies;

    private PromptStrategyBundle(String name, Set<PromptingStrategy> strategies) {
        validateExperimentalCount(strategies);
        this.name = name;
        this.strategies = Collections.unmodifiableSet(EnumSet.copyOf(strategies));
    }

    public static PromptStrategyBundle of(String name, Set<PromptingStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalArgumentException("PromptStrategyBundle의 전략 목록은 비어있을 수 없습니다.");
        }
        return new PromptStrategyBundle(name, strategies);
    }

    /** 항상 포함되는 Core 전략만으로 구성된 기본 번들 */
    public static PromptStrategyBundle coreOnly() {
        return new PromptStrategyBundle(
            "CORE_ONLY",
            EnumSet.of(
                PromptingStrategy.CLARIFY_FIRST,
                PromptingStrategy.STEP_BY_STEP,
                PromptingStrategy.CHECKLIST_VERIFY
            )
        );
    }

    private static void validateExperimentalCount(Set<PromptingStrategy> strategies) {
        long experimentalCount = strategies.stream()
                .filter(PromptingStrategy::isExperimental)
                .count();
        if (experimentalCount > 1) {
            throw new IllegalArgumentException(
                String.format(
                    "PromptStrategyBundle에 Experimental 전략은 최대 1개만 허용됩니다. 요청된 수: %d",
                    experimentalCount
                )
            );
        }
    }

    public String getName() {
        return name;
    }

    public Set<PromptingStrategy> getStrategies() {
        return strategies;
    }

    /** Core 전략 포함 여부 확인 */
    public boolean hasCoreStrategies() {
        return strategies.stream().anyMatch(PromptingStrategy::isCore);
    }

    /** Experimental 전략 목록 반환 */
    public List<PromptingStrategy> getExperimentalStrategies() {
        return strategies.stream()
                .filter(PromptingStrategy::isExperimental)
                .collect(Collectors.toList());
    }

    /** Objective-specific 전략 목록 반환 */
    public List<PromptingStrategy> getObjectiveSpecificStrategies() {
        return strategies.stream()
                .filter(s -> s.getTier() == PromptingStrategy.Tier.OBJECTIVE_SPECIFIC)
                .collect(Collectors.toList());
    }

    /** 총 추가 LLM 호출 수 계산 */
    public int totalAdditionalLlmCalls() {
        return strategies.stream()
                .mapToInt(PromptingStrategy::getAdditionalLlmCalls)
                .sum();
    }

    @Override
    public String toString() {
        return "PromptStrategyBundle{name='" + name + "', strategies=" + strategies + "}";
    }
}
