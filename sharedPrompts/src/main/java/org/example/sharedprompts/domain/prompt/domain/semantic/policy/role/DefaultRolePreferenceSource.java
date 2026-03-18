package org.example.sharedprompts.domain.prompt.domain.semantic.policy.role;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory map-backed role preference source.
 * Key: "category+intent" or "category+intent+actionKey"; value: ordered role stable keys.
 * Replaceable by classpath/json/DB without changing service code.
 */
public final class DefaultRolePreferenceSource implements RolePreferenceSource {

    private final Map<String, List<String>> preferredRoleKeysByContext;

    public DefaultRolePreferenceSource() {
        this.preferredRoleKeysByContext = Map.of();
    }

    public DefaultRolePreferenceSource(Map<String, List<String>> preferredRoleKeysByContext) {
        this.preferredRoleKeysByContext = preferredRoleKeysByContext != null
                ? Map.copyOf(preferredRoleKeysByContext)
                : Map.of();
    }

    @Override
    public List<String> getPreferredRoleKeys(PromptCategory category, ActionIntent intent) {
        if (category == null || intent == null) return Collections.emptyList();
        String key = contextKey(category, intent);
        List<String> list = preferredRoleKeysByContext.get(key);
        return list != null ? List.copyOf(list) : Collections.emptyList();
    }

    @Override
    public List<String> getPreferredRoleKeys(PromptCategory category, ActionIntent intent, String actionKey) {
        if (actionKey != null && !actionKey.isBlank()) {
            String key = contextKey(category, intent) + "+" + actionKey.trim();
            List<String> list = preferredRoleKeysByContext.get(key);
            if (list != null && !list.isEmpty()) return List.copyOf(list);
        }
        return getPreferredRoleKeys(category, intent);
    }

    private static String contextKey(PromptCategory category, ActionIntent intent) {
        return Objects.requireNonNull(category).name() + "+" + Objects.requireNonNull(intent).name();
    }
}
