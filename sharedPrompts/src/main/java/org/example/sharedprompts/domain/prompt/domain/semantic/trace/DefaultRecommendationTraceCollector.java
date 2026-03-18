package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable collector that builds RecommendationTrace.
 * Used by SemanticRecommendationService; policies receive and fill via this or via metadata.
 */
public final class DefaultRecommendationTraceCollector implements RecommendationTraceCollector {

    private final PromptCategory category;
    private final ActionIntent intent;
    private final boolean fallbackIntentUsed;

    private CompatibilityTrace compatibilityTrace;
    private ExpansionTrace expansionTrace;
    private OrderingTrace actionOrderingTrace;
    private List<ActionRecommendationTrace> actionTraces = new ArrayList<>();
    private OrderingTrace roleOrderingTrace;
    private List<RoleRecommendationTrace> roleTraces = new ArrayList<>();
    private PolicyTraceInfo policyTraceInfo;

    public DefaultRecommendationTraceCollector(PromptCategory category, ActionIntent intent, boolean fallbackIntentUsed) {
        this.category = category;
        this.intent = intent;
        this.fallbackIntentUsed = fallbackIntentUsed;
    }

    @Override
    public void recordCompatibility(List<String> allowedGroupNames, List<String> allowedActionKeys, PolicyApplicationTrace policyTrace) {
        this.compatibilityTrace = CompatibilityTrace.of(
                allowedGroupNames != null ? List.copyOf(allowedGroupNames) : List.of(),
                allowedActionKeys != null ? List.copyOf(allowedActionKeys) : List.of(),
                policyTrace
        );
    }

    @Override
    public void recordExpansion(List<String> expandedActionKeys, PolicyApplicationTrace policyTrace) {
        this.expansionTrace = ExpansionTrace.of(
                expandedActionKeys != null ? List.copyOf(expandedActionKeys) : List.of(),
                policyTrace
        );
    }

    @Override
    public void recordActionOrdering(OrderingTrace orderingTrace) {
        this.actionOrderingTrace = orderingTrace;
    }

    @Override
    public void setActionTraces(List<ActionRecommendationTrace> actionTraces) {
        this.actionTraces = actionTraces != null ? new ArrayList<>(actionTraces) : new ArrayList<>();
    }

    @Override
    public void recordRoleOrdering(OrderingTrace orderingTrace) {
        this.roleOrderingTrace = orderingTrace;
    }

    @Override
    public void setRoleTraces(List<RoleRecommendationTrace> roleTraces) {
        this.roleTraces = roleTraces != null ? new ArrayList<>(roleTraces) : new ArrayList<>();
    }

    @Override
    public void setPolicyTraceInfo(PolicyTraceInfo policyTraceInfo) {
        this.policyTraceInfo = policyTraceInfo;
    }

    @Override
    public RecommendationTrace build() {
        var builder = RecommendationTrace.builder(category, intent, fallbackIntentUsed)
                .compatibilityTrace(compatibilityTrace)
                .expansionTrace(expansionTrace)
                .actionOrderingTrace(actionOrderingTrace)
                .actionTraces(Collections.unmodifiableList(new ArrayList<>(actionTraces)))
                .roleOrderingTrace(roleOrderingTrace)
                .roleTraces(Collections.unmodifiableList(new ArrayList<>(roleTraces)));
        if (policyTraceInfo != null) {
            builder.policyTraceInfo(policyTraceInfo);
        }
        return builder.build();
    }
}
