package org.example.sharedprompts.domain.prompt.domain.semantic.impl;

import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.FallbackCandidate;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticFitLevel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable category semantic profile with optional intent fit levels and discouraged tones/styles.
 */
public final class DefaultCategorySemanticProfile implements CategorySemanticProfile {

    private final PromptCategory category;
    private final TaskDomain baseTaskDomain;
    private final Set<ActionIntent> allowedIntents;
    private final Map<ActionIntent, SemanticFitLevel> intentFitLevels;
    private final Map<ActionIntent, List<RoleTypeInterface>> recommendedRolesByIntent;
    private final Map<ActionIntent, List<ActionTypeInterface>> compatibleActionsByIntent;
    private final Map<ActionIntent, List<ToneType>> discouragedTonesByIntent;
    private final Map<ActionIntent, List<StyleType>> discouragedStylesByIntent;
    private final ActionIntent fallbackIntent;
    private final List<FallbackCandidate> fallbackCandidates;

    /**
     * Legacy constructor: all allowed intents are ALLOWED; no discouraged tones/styles.
     */
    public DefaultCategorySemanticProfile(
            PromptCategory category,
            TaskDomain baseTaskDomain,
            Set<ActionIntent> allowedIntents,
            Map<ActionIntent, List<RoleTypeInterface>> recommendedRolesByIntent,
            Map<ActionIntent, List<ActionTypeInterface>> compatibleActionsByIntent,
            ActionIntent fallbackIntent
    ) {
        this(category, baseTaskDomain, allowedIntents, null, recommendedRolesByIntent, compatibleActionsByIntent,
                null, null, fallbackIntent, null);
    }

    /**
     * Full constructor with semantic strength and discouraged tone/style.
     */
    public DefaultCategorySemanticProfile(
            PromptCategory category,
            TaskDomain baseTaskDomain,
            Set<ActionIntent> allowedIntents,
            Map<ActionIntent, SemanticFitLevel> intentFitLevels,
            Map<ActionIntent, List<RoleTypeInterface>> recommendedRolesByIntent,
            Map<ActionIntent, List<ActionTypeInterface>> compatibleActionsByIntent,
            Map<ActionIntent, List<ToneType>> discouragedTonesByIntent,
            Map<ActionIntent, List<StyleType>> discouragedStylesByIntent,
            ActionIntent fallbackIntent,
            List<FallbackCandidate> fallbackCandidates
    ) {
        this.category = category;
        this.baseTaskDomain = baseTaskDomain;
        this.allowedIntents = allowedIntents != null ? Set.copyOf(allowedIntents) : Set.of();
        this.intentFitLevels = intentFitLevels != null ? Map.copyOf(intentFitLevels) : Map.of();
        this.recommendedRolesByIntent = copyNestedLists(recommendedRolesByIntent);
        this.compatibleActionsByIntent = copyNestedLists(compatibleActionsByIntent);
        this.discouragedTonesByIntent = copyNestedLists(discouragedTonesByIntent);
        this.discouragedStylesByIntent = copyNestedLists(discouragedStylesByIntent);
        this.fallbackIntent = fallbackIntent;
        this.fallbackCandidates = fallbackCandidates != null ? List.copyOf(fallbackCandidates) : null;
    }

    private static <K, V> Map<K, List<V>> copyNestedLists(Map<K, List<V>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<K, List<V>> copy = new HashMap<>();
        source.forEach((key, value) -> copy.put(key, value == null ? List.of() : List.copyOf(value)));
        return Map.copyOf(copy);
    }

    @Override
    public PromptCategory getCategory() {
        return category;
    }

    @Override
    public TaskDomain getBaseTaskDomain() {
        return baseTaskDomain;
    }

    @Override
    public Set<ActionIntent> getAllowedIntents() {
        return allowedIntents;
    }

    @Override
    public SemanticFitLevel getIntentFitLevel(ActionIntent intent) {
        if (intent == null || !allowedIntents.contains(intent)) {
            return SemanticFitLevel.FORBIDDEN;
        }
        SemanticFitLevel level = intentFitLevels.get(intent);
        return level != null ? level : SemanticFitLevel.ALLOWED;
    }

    @Override
    public List<RoleTypeInterface> getRecommendedRolesForIntent(ActionIntent intent) {
        if (intent == null) return Collections.emptyList();
        List<RoleTypeInterface> list = recommendedRolesByIntent.get(intent);
        return list != null ? List.copyOf(list) : Collections.emptyList();
    }

    @Override
    public List<ActionTypeInterface> getCompatibleActionsForIntent(ActionIntent intent) {
        if (intent == null) return Collections.emptyList();
        List<ActionTypeInterface> list = compatibleActionsByIntent.get(intent);
        return list != null ? List.copyOf(list) : Collections.emptyList();
    }

    @Override
    public List<ToneType> getDiscouragedTonesForIntent(ActionIntent intent) {
        if (intent == null) return List.of();
        List<ToneType> list = discouragedTonesByIntent.get(intent);
        return list != null ? List.copyOf(list) : List.of();
    }

    @Override
    public List<StyleType> getDiscouragedStylesForIntent(ActionIntent intent) {
        if (intent == null) return List.of();
        List<StyleType> list = discouragedStylesByIntent.get(intent);
        return list != null ? List.copyOf(list) : List.of();
    }

    @Override
    public ActionIntent getFallbackIntent() {
        return fallbackIntent;
    }

    @Override
    public List<FallbackCandidate> getFallbackCandidates() {
        if (fallbackCandidates != null) {
            return fallbackCandidates;
        }
        return CategorySemanticProfile.super.getFallbackCandidates();
    }
}
