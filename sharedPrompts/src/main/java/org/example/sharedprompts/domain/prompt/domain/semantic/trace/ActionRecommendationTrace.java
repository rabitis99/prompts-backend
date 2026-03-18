package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Per-action trace: why this action was included, ordering reason, optional objective.
 * Internal trace model only; aligns with Category → Intent → Action → Role.
 */
public record ActionRecommendationTrace(
        String actionKey,
        String inclusionStage,
        PolicyApplicationTrace inclusionPolicy,
        Optional<OrderingTrace> orderingTrace,
        Optional<String> objectiveSourceId,
        List<RoleRecommendationTrace> roleTraces
) {
    public ActionRecommendationTrace {
        roleTraces = roleTraces != null ? List.copyOf(roleTraces) : List.of();
    }

    public static ActionRecommendationTrace of(String actionKey, String inclusionStage, PolicyApplicationTrace inclusionPolicy) {
        return new ActionRecommendationTrace(
                actionKey,
                inclusionStage,
                inclusionPolicy,
                Optional.empty(),
                Optional.empty(),
                List.of()
        );
    }

    public static ActionRecommendationTrace withOrdering(String actionKey, String inclusionStage, PolicyApplicationTrace inclusionPolicy, OrderingTrace orderingTrace) {
        return new ActionRecommendationTrace(
                actionKey,
                inclusionStage,
                inclusionPolicy,
                Optional.of(orderingTrace),
                Optional.empty(),
                List.of()
        );
    }

    public ActionRecommendationTrace withRoleTraces(List<RoleRecommendationTrace> roleTraces) {
        return new ActionRecommendationTrace(
                actionKey,
                inclusionStage,
                inclusionPolicy,
                orderingTrace,
                objectiveSourceId,
                roleTraces != null ? List.copyOf(roleTraces) : Collections.emptyList()
        );
    }
}
