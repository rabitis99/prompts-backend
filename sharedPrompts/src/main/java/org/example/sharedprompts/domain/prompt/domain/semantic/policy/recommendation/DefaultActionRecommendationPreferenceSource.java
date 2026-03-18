package org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * In-memory map-backed preference source. Key: "category.name()+intent.name()", value: ordered stable keys.
 * Replaceable by classpath/json/DB implementation. No raw map in config; extend by swapping source.
 */
public final class DefaultActionRecommendationPreferenceSource implements ActionRecommendationPreferenceSource {

    private final Map<String, List<String>> preferredByContext;

    /** Production: empty preferences; order from policy fallback (e.g. stable key). */
    public DefaultActionRecommendationPreferenceSource() {
        this.preferredByContext = Map.of();
    }

    public DefaultActionRecommendationPreferenceSource(Map<String, List<String>> preferredByContext) {
        this.preferredByContext = preferredByContext != null
                ? Map.copyOf(preferredByContext)
                : Map.of();
    }

    @Override
    public List<String> getPreferredActionKeys(PromptCategory category, ActionIntent intent) {
        if (category == null || intent == null) return Collections.emptyList();
        String key = contextKey(category, intent);
        List<String> list = preferredByContext.get(key);
        return list != null ? List.copyOf(list) : Collections.emptyList();
    }

    private static String contextKey(PromptCategory category, ActionIntent intent) {
        return Objects.requireNonNull(category).name() + "+" + Objects.requireNonNull(intent).name();
    }
}
