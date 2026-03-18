package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Trace for ordering step: which source defined order, fallback applied, per-item rank source.
 * Internal trace model only.
 */
public record OrderingTrace(
        PolicyApplicationTrace policyTrace,
        boolean fallbackOrderingApplied,
        List<OrderedItemTrace> orderedItems
) {
    public OrderingTrace {
        orderedItems = orderedItems != null ? List.copyOf(orderedItems) : List.of();
    }

    /** Per-item: identifier and whether order came from preference or fallback. */
    public record OrderedItemTrace(String itemKey, String rankSource, boolean fromFallback) {}

    public static OrderingTrace of(PolicyApplicationTrace policyTrace, boolean fallbackOrderingApplied, List<OrderedItemTrace> orderedItems) {
        return new OrderingTrace(
                policyTrace,
                fallbackOrderingApplied,
                orderedItems != null ? List.copyOf(orderedItems) : Collections.emptyList()
        );
    }
}
