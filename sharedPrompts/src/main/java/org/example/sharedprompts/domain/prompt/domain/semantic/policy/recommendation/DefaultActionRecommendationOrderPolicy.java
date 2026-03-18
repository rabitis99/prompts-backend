package org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.OrderingTrace;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.PolicyApplicationTrace;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTraceCollector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Applies order from {@link ActionRecommendationPreferenceSource}, then fallback by stable key.
 * Recommendation order is never from seed or registry order.
 * When collector is provided, records ordering trace with per-action preference vs fallback.
 */
public final class DefaultActionRecommendationOrderPolicy implements ActionRecommendationOrderPolicy {

    private static final String SOURCE_KIND = "in-memory";
    private static final String SOURCE_ID = "DefaultActionRecommendationPreferenceSource";
    private static final String POLICY_TYPE = "ActionRecommendationOrder";

    private final ActionRecommendationPreferenceSource preferenceSource;

    public DefaultActionRecommendationOrderPolicy(ActionRecommendationPreferenceSource preferenceSource) {
        this.preferenceSource = Objects.requireNonNull(preferenceSource, "preferenceSource");
    }

    @Override
    public List<ActionTypeInterface> applyOrder(
            PromptCategory category,
            ActionIntent intent,
            List<ActionTypeInterface> candidates
    ) {
        return applyOrder(category, intent, candidates, null);
    }

    @Override
    public List<ActionTypeInterface> applyOrder(
            PromptCategory category,
            ActionIntent intent,
            List<ActionTypeInterface> candidates,
            RecommendationTraceCollector collector
    ) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        List<String> preferredKeys = preferenceSource.getPreferredActionKeys(category, intent);
        Map<String, Integer> positionByKey = new LinkedHashMap<>();
        int idx = 0;
        for (String key : preferredKeys) {
            if (key != null && !key.isBlank()) {
                positionByKey.putIfAbsent(key.trim(), idx++);
            }
        }
        boolean hasPreference = !positionByKey.isEmpty();

        Comparator<ActionTypeInterface> comparator = (a, b) -> {
            String keyA = a != null ? a.key() : null;
            String keyB = b != null ? b.key() : null;
            Integer posA = keyA != null ? positionByKey.get(keyA) : null;
            Integer posB = keyB != null ? positionByKey.get(keyB) : null;
            if (posA != null && posB != null) return Integer.compare(posA, posB);
            if (posA != null) return -1;
            if (posB != null) return 1;
            return Comparator.<String>nullsFirst(Comparator.naturalOrder()).compare(keyA, keyB);
        };

        List<ActionTypeInterface> mutable = new ArrayList<>(candidates);
        mutable.sort(comparator);

        if (collector != null) {
            List<OrderingTrace.OrderedItemTrace> items = new ArrayList<>();
            for (ActionTypeInterface a : mutable) {
                String key = a != null ? a.key() : null;
                if (key == null) continue;
                boolean fromFallback = !positionByKey.containsKey(key);
                String rankSource = fromFallback ? "fallback-stable-key" : "preference-source";
                items.add(new OrderingTrace.OrderedItemTrace(key, rankSource, fromFallback));
            }
            PolicyApplicationTrace policyTrace = PolicyApplicationTrace.of(
                    SOURCE_KIND, SOURCE_ID, POLICY_TYPE, hasPreference ? false : true
            );
            OrderingTrace orderingTrace = OrderingTrace.of(policyTrace, hasPreference ? false : true, items);
            collector.recordActionOrdering(orderingTrace);
        }

        return List.copyOf(mutable);
    }
}
