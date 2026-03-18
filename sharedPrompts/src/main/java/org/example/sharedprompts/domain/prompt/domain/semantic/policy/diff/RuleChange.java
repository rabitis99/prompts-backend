package org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff;

import java.util.Objects;

/**
 * Single rule-level change between two policy documents.
 * ruleIdentifier is stable (e.g. context key "CATEGORY+INTENT" or mapping key).
 */
public record RuleChange(
        RuleChangeType changeType,
        String policyFamily,
        String ruleIdentifier,
        Object oldValue,
        Object newValue
) {
    public RuleChange {
        Objects.requireNonNull(changeType, "changeType");
        if (policyFamily == null || policyFamily.isBlank()) {
            throw new IllegalArgumentException("policyFamily is required");
        }
        if (ruleIdentifier == null || ruleIdentifier.isBlank()) {
            throw new IllegalArgumentException("ruleIdentifier is required");
        }
    }

    public static RuleChange add(String policyFamily, String ruleIdentifier, Object newValue) {
        return new RuleChange(RuleChangeType.ADD, policyFamily, ruleIdentifier, null, newValue);
    }

    public static RuleChange remove(String policyFamily, String ruleIdentifier, Object oldValue) {
        return new RuleChange(RuleChangeType.REMOVE, policyFamily, ruleIdentifier, oldValue, null);
    }

    public static RuleChange modify(String policyFamily, String ruleIdentifier, Object oldValue, Object newValue) {
        return new RuleChange(RuleChangeType.MODIFY, policyFamily, ruleIdentifier, oldValue, newValue);
    }

    public enum RuleChangeType {
        ADD,
        REMOVE,
        MODIFY
    }
}
