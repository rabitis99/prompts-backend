package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable seed data for one category's semantic profile.
 * Source-backed; registry assembles {@link CategorySemanticProfile} from this, not from inline data.
 */
public record CategorySemanticProfileSeed(
        PromptCategory category,
        TaskDomain taskDomain,
        Set<ActionIntent> allowedIntents,
        Map<ActionIntent, SemanticFitLevel> intentFitLevels,
        Map<ActionIntent, List<RoleTypeInterface>> rolesByIntent,
        Map<ActionIntent, List<ActionTypeInterface>> actionsByIntent,
        Map<ActionIntent, List<ToneType>> discouragedTonesByIntent,
        Map<ActionIntent, List<StyleType>> discouragedStylesByIntent,
        ActionIntent fallbackIntent,
        List<FallbackCandidate> fallbackCandidates
) {
    public CategorySemanticProfileSeed {
        allowedIntents = allowedIntents != null ? Set.copyOf(allowedIntents) : Set.of();
        intentFitLevels = intentFitLevels != null ? Map.copyOf(intentFitLevels) : Map.of();
        rolesByIntent = copyNested(rolesByIntent);
        actionsByIntent = copyNested(actionsByIntent);
        discouragedTonesByIntent = copyNested(discouragedTonesByIntent);
        discouragedStylesByIntent = copyNested(discouragedStylesByIntent);
        fallbackCandidates = fallbackCandidates != null ? List.copyOf(fallbackCandidates) : List.of();
    }

    private static <K, V> Map<K, List<V>> copyNested(Map<K, List<V>> source) {
        if (source == null || source.isEmpty()) return Map.of();
        Map<K, List<V>> copy = new HashMap<>();
        source.forEach((k, v) -> copy.put(k, v == null ? List.of() : List.copyOf(v)));
        return Collections.unmodifiableMap(copy);
    }
}
