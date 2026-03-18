package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of recommendation trace for audit/decisions.
 * Includes policy version/source/experiment for reproducibility.
 * Safe to serialize (e.g. JSON) or store; does not hold domain enum references that might change.
 */
public record RecommendationTraceSnapshot(
        String categoryKey,
        String intentKey,
        boolean fallbackIntentUsed,
        List<String> compatibilitySourceIds,
        List<String> expansionSourceIds,
        String orderingSourceId,
        boolean orderingFallbackApplied,
        List<String> actionKeysInOrder,
        List<String> roleKeysInOrder,
        String policyVersionId,
        String policySourceId,
        String experimentId,
        String variantId
) {
    public RecommendationTraceSnapshot {
        compatibilitySourceIds = compatibilitySourceIds != null ? List.copyOf(compatibilitySourceIds) : List.of();
        expansionSourceIds = expansionSourceIds != null ? List.copyOf(expansionSourceIds) : List.of();
        actionKeysInOrder = actionKeysInOrder != null ? List.copyOf(actionKeysInOrder) : List.of();
        roleKeysInOrder = roleKeysInOrder != null ? List.copyOf(roleKeysInOrder) : List.of();
    }

    public static RecommendationTraceSnapshot from(RecommendationTrace trace) {
        String categoryKey = trace.category() != null ? trace.category().name() : null;
        String intentKey = trace.intent() != null ? trace.intent().name() : null;
        List<String> compatibilitySourceIds = trace.compatibilityTrace()
                .map(ct -> List.of(ct.policyTrace().sourceId()))
                .orElse(Collections.emptyList());
        List<String> expansionSourceIds = trace.expansionTrace()
                .map(et -> List.of(et.policyTrace().sourceId()))
                .orElse(Collections.emptyList());
        String orderingSourceId = trace.actionOrderingTrace()
                .map(ot -> ot.policyTrace().sourceId())
                .orElse(null);
        boolean orderingFallbackApplied = trace.actionOrderingTrace()
                .map(OrderingTrace::fallbackOrderingApplied)
                .orElse(false);
        List<String> actionKeysInOrder = trace.actionOrderingTrace()
                .map(ot -> ot.orderedItems().stream().map(OrderingTrace.OrderedItemTrace::itemKey).toList())
                .orElseGet(() -> trace.actionTraces().stream().map(ActionRecommendationTrace::actionKey).toList());
        List<String> roleKeysInOrder = trace.roleTraces().stream()
                .map(RoleRecommendationTrace::roleKey)
                .toList();

        String policyVersionId = null;
        String policySourceId = null;
        String experimentId = null;
        String variantId = null;
        if (trace.policyTraceInfo() != null && trace.policyTraceInfo().isPresent()) {
            PolicyTraceInfo info = trace.policyTraceInfo().get();
            policyVersionId = info.policyVersionId();
            policySourceId = info.policySourceId();
            experimentId = info.experimentId().orElse(null);
            variantId = info.variantId().orElse(null);
        }

        return new RecommendationTraceSnapshot(
                categoryKey,
                intentKey,
                trace.fallbackIntentUsed(),
                compatibilitySourceIds,
                expansionSourceIds,
                orderingSourceId,
                orderingFallbackApplied,
                actionKeysInOrder,
                roleKeysInOrder,
                policyVersionId,
                policySourceId,
                experimentId,
                variantId
        );
    }
}
