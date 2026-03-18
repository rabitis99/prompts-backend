package org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Expands allowed groups to concrete actions via {@link ConcreteActionByGroupIndex}.
 * Does not use registry.getAll() or catalog order. Order of returned list is undefined;
 * caller must apply {@link ActionRecommendationOrderPolicy}.
 */
public final class DefaultRecommendationConcreteActionExpander implements RecommendationConcreteActionExpander {

    private final ConcreteActionByGroupIndex actionByGroupIndex;

    public DefaultRecommendationConcreteActionExpander(ConcreteActionByGroupIndex actionByGroupIndex) {
        this.actionByGroupIndex = Objects.requireNonNull(actionByGroupIndex, "actionByGroupIndex");
    }

    @Override
    public List<ActionTypeInterface> expand(
            PromptCategory category,
            ActionIntent intent,
            Set<ActionGroup> allowedGroups,
            List<ActionTypeInterface> seedActions
    ) {
        LinkedHashSet<ActionTypeInterface> result = new LinkedHashSet<>();
        if (seedActions != null) {
            for (ActionTypeInterface a : seedActions) {
                if (a != null && allowedGroups != null && allowedGroups.contains(a.getActionGroup())) {
                    result.add(a);
                }
            }
        }
        if (allowedGroups != null) {
            for (ActionGroup group : allowedGroups) {
                for (ActionTypeInterface a : actionByGroupIndex.getActionsByGroup(group)) {
                    result.add(a);
                }
            }
        }
        return new ArrayList<>(result);
    }
}
