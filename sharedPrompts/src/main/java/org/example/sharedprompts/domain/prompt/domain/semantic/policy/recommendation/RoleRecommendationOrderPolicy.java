package org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTraceCollector;

import java.util.List;

/**
 * Explicit policy for role recommendation order. Same input must yield same order.
 * Order must not come from profile list order, enum order, or insertion order.
 * When collector is provided, policy records ordering trace (preference vs fallback per role).
 */
public interface RoleRecommendationOrderPolicy {

    /**
     * Returns the same candidates in the order defined by this policy.
     *
     * @param category   context category
     * @param intent     context intent
     * @param candidates unordered or arbitrarily ordered role candidates
     * @return list with same elements, in recommendation order
     */
    List<RoleTypeInterface> applyOrder(
            PromptCategory category,
            ActionIntent intent,
            List<RoleTypeInterface> candidates
    );

    /**
     * Same as {@link #applyOrder(PromptCategory, ActionIntent, List)} but records ordering trace
     * into collector when non-null (source id, fallback applied per role).
     */
    default List<RoleTypeInterface> applyOrder(
            PromptCategory category,
            ActionIntent intent,
            List<RoleTypeInterface> candidates,
            RecommendationTraceCollector collector
    ) {
        return applyOrder(category, intent, candidates);
    }
}
