package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Root internal trace for a single recommendation run.
 * Contains phase-wise traces: compatibility → expansion → ordering → role.
 * Policy version/source/experiment recorded for audit and reproducibility.
 * Not exposed as-is to external API; use RecommendationExplanationAssembler for user-facing explanation.
 */
public record RecommendationTrace(
        PromptCategory category,
        ActionIntent intent,
        Optional<CompatibilityTrace> compatibilityTrace,
        Optional<ExpansionTrace> expansionTrace,
        Optional<OrderingTrace> actionOrderingTrace,
        List<ActionRecommendationTrace> actionTraces,
        Optional<OrderingTrace> roleOrderingTrace,
        List<RoleRecommendationTrace> roleTraces,
        boolean fallbackIntentUsed,
        Optional<PolicyTraceInfo> policyTraceInfo
) {
    public RecommendationTrace {
        actionTraces = actionTraces != null ? List.copyOf(actionTraces) : List.of();
        roleTraces = roleTraces != null ? List.copyOf(roleTraces) : List.of();
        policyTraceInfo = policyTraceInfo != null ? policyTraceInfo : Optional.empty();
    }

    public static Builder builder(PromptCategory category, ActionIntent intent, boolean fallbackIntentUsed) {
        return new Builder(category, intent, fallbackIntentUsed);
    }

    public static final class Builder {
        private final PromptCategory category;
        private final ActionIntent intent;
        private final boolean fallbackIntentUsed;
        private CompatibilityTrace compatibilityTrace;
        private ExpansionTrace expansionTrace;
        private OrderingTrace actionOrderingTrace;
        private List<ActionRecommendationTrace> actionTraces = List.of();
        private OrderingTrace roleOrderingTrace;
        private List<RoleRecommendationTrace> roleTraces = List.of();
        private PolicyTraceInfo policyTraceInfo;

        Builder(PromptCategory category, ActionIntent intent, boolean fallbackIntentUsed) {
            this.category = category;
            this.intent = intent;
            this.fallbackIntentUsed = fallbackIntentUsed;
        }

        public Builder compatibilityTrace(CompatibilityTrace t) { this.compatibilityTrace = t; return this; }
        public Builder expansionTrace(ExpansionTrace t) { this.expansionTrace = t; return this; }
        public Builder actionOrderingTrace(OrderingTrace t) { this.actionOrderingTrace = t; return this; }
        public Builder actionTraces(List<ActionRecommendationTrace> t) { this.actionTraces = t != null ? List.copyOf(t) : List.of(); return this; }
        public Builder roleOrderingTrace(OrderingTrace t) { this.roleOrderingTrace = t; return this; }
        public Builder roleTraces(List<RoleRecommendationTrace> t) { this.roleTraces = t != null ? List.copyOf(t) : List.of(); return this; }
        public Builder policyTraceInfo(PolicyTraceInfo t) { this.policyTraceInfo = t; return this; }

        public RecommendationTrace build() {
            return new RecommendationTrace(
                    category,
                    intent,
                    Optional.ofNullable(compatibilityTrace),
                    Optional.ofNullable(expansionTrace),
                    Optional.ofNullable(actionOrderingTrace),
                    actionTraces != null ? actionTraces : Collections.emptyList(),
                    Optional.ofNullable(roleOrderingTrace),
                    roleTraces != null ? roleTraces : Collections.emptyList(),
                    fallbackIntentUsed,
                    Optional.ofNullable(policyTraceInfo)
            );
        }
    }
}
