package org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource;
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
 * Applies order from {@link RolePreferenceSource}, then fallback by stable key.
 * Role recommendation order is never from profile list order or enum order.
 * When collector is provided, records ordering trace with per-role preference vs fallback.
 */
public final class DefaultRoleRecommendationOrderPolicy implements RoleRecommendationOrderPolicy {

    private static final String SOURCE_KIND = "in-memory";
    private static final String SOURCE_ID = "DefaultRolePreferenceSource";
    private static final String POLICY_TYPE = "RoleRecommendationOrder";

    private final RolePreferenceSource preferenceSource;

    public DefaultRoleRecommendationOrderPolicy(RolePreferenceSource preferenceSource) {
        this.preferenceSource = Objects.requireNonNull(preferenceSource, "preferenceSource");
    }

    @Override
    public List<RoleTypeInterface> applyOrder(
            PromptCategory category,
            ActionIntent intent,
            List<RoleTypeInterface> candidates
    ) {
        return applyOrder(category, intent, candidates, null);
    }

    @Override
    public List<RoleTypeInterface> applyOrder(
            PromptCategory category,
            ActionIntent intent,
            List<RoleTypeInterface> candidates,
            RecommendationTraceCollector collector
    ) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        List<String> preferredKeys = preferenceSource.getPreferredRoleKeys(category, intent);
        Map<String, Integer> positionByKey = new LinkedHashMap<>();
        int idx = 0;
        for (String key : preferredKeys) {
            if (key != null && !key.isBlank()) {
                positionByKey.putIfAbsent(key.trim(), idx++);
            }
        }
        boolean hasPreference = !positionByKey.isEmpty();

        Comparator<RoleTypeInterface> comparator = (a, b) -> {
            String keyA = a != null ? a.key() : null;
            String keyB = b != null ? b.key() : null;
            Integer posA = keyA != null ? positionByKey.get(keyA) : null;
            Integer posB = keyB != null ? positionByKey.get(keyB) : null;
            if (posA != null && posB != null) return Integer.compare(posA, posB);
            if (posA != null) return -1;
            if (posB != null) return 1;
            return Comparator.<String>nullsFirst(Comparator.naturalOrder()).compare(keyA, keyB);
        };

        List<RoleTypeInterface> mutable = new ArrayList<>(candidates);
        mutable.sort(comparator);

        if (collector != null) {
            List<OrderingTrace.OrderedItemTrace> items = new ArrayList<>();
            for (RoleTypeInterface r : mutable) {
                String key = r != null ? r.key() : null;
                if (key == null) continue;
                boolean fromFallback = !positionByKey.containsKey(key);
                String rankSource = fromFallback ? "fallback-stable-key" : "preference-source";
                items.add(new OrderingTrace.OrderedItemTrace(key, rankSource, fromFallback));
            }
            PolicyApplicationTrace policyTrace = PolicyApplicationTrace.of(
                    SOURCE_KIND, SOURCE_ID, POLICY_TYPE, hasPreference ? false : true
            );
            OrderingTrace orderingTrace = OrderingTrace.of(policyTrace, hasPreference ? false : true, items);
            collector.recordRoleOrdering(orderingTrace);
        }

        return List.copyOf(mutable);
    }
}
