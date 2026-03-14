package org.example.sharedprompts.domain.prompt.common.enums.action.resolver;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;

import java.util.Objects;
import java.util.Optional;

/** ActionType 공식 해석: stable key 우선 → 실패 시 레거시 위임. 정책/메타/그룹 판단 없음. */
public final class DefaultActionTypeResolver implements ActionTypeResolver {

    private final ActionTypeRegistry registry;
    private final ActionTypeCompatibilityResolver compatibility;

    public DefaultActionTypeResolver(ActionTypeRegistry registry, ActionTypeCompatibilityResolver compatibility) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.compatibility = Objects.requireNonNull(compatibility, "compatibility");
    }

    @Override
    public Optional<ActionTypeInterface> resolve(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null or blank");
        }
        String trimmed = value.trim();
        ActionTypeInterface byStableKey = registry.getByStableKey(trimmed);
        if (byStableKey != null) {
            return Optional.of(byStableKey);
        }
        try {
            return Optional.of(compatibility.resolve(trimmed));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
