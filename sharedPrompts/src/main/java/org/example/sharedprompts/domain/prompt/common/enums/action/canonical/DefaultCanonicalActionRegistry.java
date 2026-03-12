package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation that maps every ActionType key to a canonical action.
 * Map is built at class load from {@link ActionTypeCatalog}; every key must be assigned.
 */
@Component
public class DefaultCanonicalActionRegistry implements CanonicalActionRegistry {

    private static final Map<String, CanonicalActionId> KEY_TO_CANONICAL = buildKeyToCanonical();

    private static Map<String, CanonicalActionId> buildKeyToCanonical() {
        Map<String, CanonicalActionId> map = new HashMap<>();
        for (Class<? extends Enum<?>> enumClass : ActionTypeCatalog.ACTION_ENUMS) {
            if (!ActionTypeInterface.class.isAssignableFrom(enumClass)) {
                continue;
            }
            String enumName = enumClass.getSimpleName();
            Enum<?>[] constants = enumClass.getEnumConstants();
            if (constants == null) continue;
            for (Enum<?> c : constants) {
                ActionTypeInterface action = (ActionTypeInterface) c;
                String key = action.key();
                CanonicalActionId canonical = ActionToCanonicalMapping.canonicalFor(enumName, c.name());
                if (canonical == null) {
                    throw new IllegalStateException("No canonical mapping for " + enumName + "." + c.name() + " (key=" + key + ")");
                }
                map.put(key, canonical);
            }
        }
        return Map.copyOf(map);
    }

    @Override
    public Optional<CanonicalActionId> toCanonical(ActionTypeInterface actionType) {
        if (actionType == null) return Optional.empty();
        return Optional.ofNullable(KEY_TO_CANONICAL.get(actionType.key()));
    }

    @Override
    public Optional<CanonicalActionId> findByKey(String stableKey) {
        if (stableKey == null || stableKey.isBlank()) return Optional.empty();
        return Optional.ofNullable(KEY_TO_CANONICAL.get(stableKey.trim()));
    }
}
