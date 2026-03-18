package org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff;

import java.util.List;
import java.util.Objects;

/**
 * Structural diff between two policy document sets (by version).
 * Document-based; rule identifiers are stable for audit and migration.
 */
public record PolicyDiff(
        String fromVersion,
        String toVersion,
        String changeSummary,
        List<RuleChange> ruleChanges
) {
    public PolicyDiff {
        Objects.requireNonNull(fromVersion, "fromVersion");
        Objects.requireNonNull(toVersion, "toVersion");
        changeSummary = changeSummary != null ? changeSummary : "";
        ruleChanges = ruleChanges != null ? List.copyOf(ruleChanges) : List.of();
    }

    public boolean isEmpty() {
        return ruleChanges.isEmpty();
    }
}
