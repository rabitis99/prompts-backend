package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import java.util.Optional;

/**
 * Per-role trace: why this role was recommended for the parent action/intent.
 * Internal trace model only.
 */
public record RoleRecommendationTrace(
        String roleKey,
        String inclusionStage,
        PolicyApplicationTrace inclusionPolicy,
        Optional<OrderingTrace> orderingTrace
) {
    public static RoleRecommendationTrace of(String roleKey, String inclusionStage, PolicyApplicationTrace inclusionPolicy) {
        return new RoleRecommendationTrace(roleKey, inclusionStage, inclusionPolicy, Optional.empty());
    }

    public static RoleRecommendationTrace withOrdering(String roleKey, String inclusionStage, PolicyApplicationTrace inclusionPolicy, OrderingTrace orderingTrace) {
        return new RoleRecommendationTrace(roleKey, inclusionStage, inclusionPolicy, Optional.of(orderingTrace));
    }
}
