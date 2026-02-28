package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.domain.value.PromptObjective;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

/**
 * Objective 해석 구현: actionType 기본값 → 명시/휴리스틱/도메인기본.
 *
 * <p>{@link ObjectiveResolverPort} 구현체. {@link ObjectiveMappingRegistryPort} 하나만 사용.
 */
public class ObjectiveResolver implements ObjectiveResolverPort {

    private final ObjectiveMappingRegistryPort mappingRegistry;

    public ObjectiveResolver(ObjectiveMappingRegistryPort mappingRegistry) {
        this.mappingRegistry = mappingRegistry;
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
            var fromRegistry = mappingRegistry.findByActionType(actionType);
            if (fromRegistry.isPresent()) {
                return fromRegistry.get();
            }
        }

        return mappingRegistry.getDomainDefault(effectiveDomain);
    }
}
