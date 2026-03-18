package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/** ActionType의 ActionGroup을 조회하는 기본 구현. */
@Component
public class DefaultCanonicalActionRegistry implements CanonicalActionRegistry {

    private final ActionTypeRegistry actionTypeRegistry;

    public DefaultCanonicalActionRegistry(ActionTypeRegistry actionTypeRegistry) {
        this.actionTypeRegistry = Objects.requireNonNull(actionTypeRegistry, "actionTypeRegistry");
    }

    /**
     * Definition-first: canonical meaning comes from the ActionType's own {@link ActionTypeInterface#getActionGroup()}.
     * No registry re-query — registry is used only for {@link #findByKey(String)} when resolving by stable key.
     */
    @Override
    public Optional<ActionGroup> toCanonical(ActionTypeInterface actionType) {
        if (actionType == null) {
            return Optional.empty();
        }
        ActionGroup group = actionType.getActionGroup();
        if (group != null) {
            return Optional.of(group);
        }
        return findByKey(actionType.key());
    }

    @Override
    public Optional<ActionGroup> findByKey(String stableKey) {
        if (stableKey == null || stableKey.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(actionTypeRegistry.getByStableKey(stableKey))
                .map(ActionTypeInterface::getActionGroup);
    }
}