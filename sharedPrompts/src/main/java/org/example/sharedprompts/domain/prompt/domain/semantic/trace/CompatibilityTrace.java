package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import java.util.Collections;
import java.util.List;

/**
 * Trace for compatibility phase: which source allowed which groups/actions.
 * Internal trace model only.
 */
public record CompatibilityTrace(
        List<String> allowedGroupNames,
        List<String> allowedActionKeys,
        PolicyApplicationTrace policyTrace
) {
    public CompatibilityTrace {
        allowedGroupNames = allowedGroupNames != null ? List.copyOf(allowedGroupNames) : List.of();
        allowedActionKeys = allowedActionKeys != null ? List.copyOf(allowedActionKeys) : List.of();
    }

    public static CompatibilityTrace of(List<String> allowedGroupNames, List<String> allowedActionKeys, PolicyApplicationTrace policyTrace) {
        return new CompatibilityTrace(
                allowedGroupNames != null ? List.copyOf(allowedGroupNames) : Collections.emptyList(),
                allowedActionKeys != null ? List.copyOf(allowedActionKeys) : Collections.emptyList(),
                policyTrace
        );
    }
}
