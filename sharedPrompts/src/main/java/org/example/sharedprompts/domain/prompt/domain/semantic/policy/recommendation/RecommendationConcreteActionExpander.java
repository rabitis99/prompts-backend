package org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;
import java.util.Set;

/**
 * Expands allowed canonical/groups into concrete action candidates for recommendation.
 * Does not define order; {@link ActionRecommendationOrderPolicy} applies order.
 * Must not depend on profile seed declaration order or registry/catalog iteration order.
 */
public interface RecommendationConcreteActionExpander {

    /**
     * Produces concrete action candidates for (category, intent) given allowed groups.
     *
     * @param category   context category
     * @param intent     context intent
     * @param allowedGroups compatible action groups from profile (semantic compatibility)
     * @param seedActions   optional seeds from profile; included in result, rest from group expansion
     * @return list of concrete actions to recommend (order not defined; apply ordering policy after)
     */
    List<ActionTypeInterface> expand(
            PromptCategory category,
            ActionIntent intent,
            Set<ActionGroup> allowedGroups,
            List<ActionTypeInterface> seedActions
    );
}
