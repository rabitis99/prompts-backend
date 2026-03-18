package org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Result of analyzing how a policy change affects recommendation behavior.
 * Heuristic-based; supports review/approval and rollback decisions.
 */
public record PolicyChangeImpact(
        List<RuleChange> breakingChanges,
        List<RuleChange> safeChanges,
        List<RuleChange> behaviorChanges,
        Set<String> affectedCategories,
        Set<String> affectedIntents,
        Set<String> affectedActions
) {
    public PolicyChangeImpact {
        breakingChanges = breakingChanges != null ? List.copyOf(breakingChanges) : List.of();
        safeChanges = safeChanges != null ? List.copyOf(safeChanges) : List.of();
        behaviorChanges = behaviorChanges != null ? List.copyOf(behaviorChanges) : List.of();
        affectedCategories = affectedCategories != null ? Set.copyOf(affectedCategories) : Set.of();
        affectedIntents = affectedIntents != null ? Set.copyOf(affectedIntents) : Set.of();
        affectedActions = affectedActions != null ? Set.copyOf(affectedActions) : Set.of();
    }

    public boolean hasBreakingChanges() {
        return !breakingChanges.isEmpty();
    }
}
