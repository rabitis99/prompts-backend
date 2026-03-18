package org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory map-backed compatibility policy source.
 * Key: "category.name()+intent.name()", value: ordered list of action group stable keys.
 * Replaceable by classpath/json/DB without changing profile or service code.
 */
public final class DefaultCompatibilityPolicySource implements CompatibilityPolicySource {

    private final Map<String, List<String>> compatibleGroupKeysByContext;

    public DefaultCompatibilityPolicySource() {
        this.compatibleGroupKeysByContext = Map.of();
    }

    public DefaultCompatibilityPolicySource(Map<String, List<String>> compatibleGroupKeysByContext) {
        this.compatibleGroupKeysByContext = compatibleGroupKeysByContext != null
                ? Map.copyOf(compatibleGroupKeysByContext)
                : Map.of();
    }

    @Override
    public List<String> getCompatibleGroupKeys(PromptCategory category, ActionIntent intent) {
        if (category == null || intent == null) {
            return List.of();
        }
        String key = contextKey(category, intent);
        List<String> list = compatibleGroupKeysByContext.get(key);
        return list != null ? List.copyOf(list) : List.of();
    }

    private static String contextKey(PromptCategory category, ActionIntent intent) {
        return Objects.requireNonNull(category).name() + "+" + Objects.requireNonNull(intent).name();
    }
}
