package org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

import java.util.List;

/**
 * Index of concrete actions by {@link ActionGroup}.
 * Built once at bootstrap from registry; used by {@link RecommendationConcreteActionExpander}
 * so that expansion does not iterate registry. Order of list per group is undefined;
 * {@link ActionRecommendationOrderPolicy} defines recommendation order.
 */
public interface ConcreteActionByGroupIndex {

    /**
     * Returns all concrete action types that belong to the given group.
     * Order is not defined; do not use for recommendation ordering.
     */
    List<ActionTypeInterface> getActionsByGroup(ActionGroup group);
}
