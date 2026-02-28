package org.example.sharedprompts.domain.prompt.domain.resolution;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

/**
 * Objective 해석 구현: 명시 오버라이드 → 명시 매핑 → 휴리스틱 → 도메인 기본값.
 *
 * <p>{@link ObjectiveResolverPort}의 단일 구현. 휴리스틱은 호환용 fallback.
 */
public class ObjectiveResolver implements ObjectiveResolverPort {

    private final ExplicitObjectiveMappingPort explicitMapping;
    private final ObjectiveMappingRegistryPort fallbackRegistry;

    public ObjectiveResolver(ExplicitObjectiveMappingPort explicitMapping,
                             ObjectiveMappingRegistryPort fallbackRegistry) {
        this.explicitMapping = explicitMapping;
        this.fallbackRegistry = fallbackRegistry;
    }

    @Override
    public PromptObjective resolve(TaskDomain taskDomain, ActionTypeInterface actionType) {
        TaskDomain effectiveDomain = taskDomain != null ? taskDomain : TaskDomain.GENERAL;

        if (actionType != null) {
            PromptObjective defaultObjective = actionType.getDefaultObjective();
            if (defaultObjective != null) {
                return defaultObjective;
            }
        }

        if (actionType != null) {
            var fromExplicit = explicitMapping.get(actionType);
            if (fromExplicit.isPresent()) {
                return fromExplicit.get();
            }
        }

        if (actionType != null) {
            var fromHeuristic = fallbackRegistry.findByActionType(actionType);
            if (fromHeuristic.isPresent()) {
                return fromHeuristic.get();
            }
        }

        return fallbackRegistry.getDomainDefault(effectiveDomain);
    }
}
