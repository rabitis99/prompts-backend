package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import java.util.Collections;
import java.util.List;

/**
 * Trace for concrete expansion phase: which groups were expanded, which source did it.
 * Internal trace model only.
 */
public record ExpansionTrace(
        List<String> expandedActionKeys,
        PolicyApplicationTrace policyTrace
) {
    public ExpansionTrace {
        expandedActionKeys = expandedActionKeys != null ? List.copyOf(expandedActionKeys) : List.of();
    }

    public static ExpansionTrace of(List<String> expandedActionKeys, PolicyApplicationTrace policyTrace) {
        return new ExpansionTrace(
                expandedActionKeys != null ? List.copyOf(expandedActionKeys) : Collections.emptyList(),
                policyTrace
        );
    }
}
