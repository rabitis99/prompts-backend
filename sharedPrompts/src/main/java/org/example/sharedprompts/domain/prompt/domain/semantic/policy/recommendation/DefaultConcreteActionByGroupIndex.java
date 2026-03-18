package org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds group → concrete actions index from a snapshot of action types.
 * Constructed once at config time; no registry iteration in recommendation path.
 */
public final class DefaultConcreteActionByGroupIndex implements ConcreteActionByGroupIndex {

    private final Map<ActionGroup, List<ActionTypeInterface>> byGroup;

    public DefaultConcreteActionByGroupIndex(List<ActionTypeInterface> allActions) {
        Objects.requireNonNull(allActions, "allActions");
        Map<ActionGroup, List<ActionTypeInterface>> map = new HashMap<>();
        for (ActionTypeInterface action : allActions) {
            ActionGroup group = action.getActionGroup();
            if (group == null) continue;
            map.computeIfAbsent(group, g -> new ArrayList<>()).add(action);
        }
        Map<ActionGroup, List<ActionTypeInterface>> immutable = new HashMap<>();
        map.forEach((g, list) -> immutable.put(g, List.copyOf(list)));
        this.byGroup = Collections.unmodifiableMap(immutable);
    }

    @Override
    public List<ActionTypeInterface> getActionsByGroup(ActionGroup group) {
        if (group == null) return List.of();
        List<ActionTypeInterface> list = byGroup.get(group);
        return list != null ? list : List.of();
    }
}
