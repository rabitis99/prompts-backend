package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Structured record of a single policy application for audit/explainability.
 * Internal only; not exposed as-is to external API.
 */
public record PolicyApplicationTrace(
        String sourceKind,
        String sourceId,
        String sourceName,
        String policyType,
        Optional<String> ruleId,
        boolean fallbackApplied,
        List<String> appliedPreferenceKeys
) {
    public PolicyApplicationTrace {
        appliedPreferenceKeys = appliedPreferenceKeys != null ? List.copyOf(appliedPreferenceKeys) : Collections.emptyList();
    }

    public static PolicyApplicationTrace of(String sourceKind, String sourceId, String policyType, boolean fallbackApplied) {
        return new PolicyApplicationTrace(
                sourceKind,
                sourceId,
                sourceId,
                policyType,
                Optional.empty(),
                fallbackApplied,
                List.of()
        );
    }
}
