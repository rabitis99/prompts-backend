package org.example.sharedprompts.domain.prompt.domain.semantic.observability;

import java.util.List;
import java.util.Optional;

/**
 * Standard dimensions for recommendation metrics and evaluation.
 * Shared by online metrics and offline evaluation so dashboards and pipelines use the same axes.
 */
public record PolicyMetricsDimensions(
        String categoryKey,
        String intentKey,
        String actionKey,
        String roleKey,
        String policyVersion,
        String experimentId,
        String variantId,
        String sourceKind,
        boolean fallbackApplied
) {
    public PolicyMetricsDimensions {
        categoryKey = nullToEmpty(categoryKey);
        intentKey = nullToEmpty(intentKey);
        actionKey = nullToEmpty(actionKey);
        roleKey = nullToEmpty(roleKey);
        policyVersion = nullToEmpty(policyVersion);
        experimentId = nullToEmpty(experimentId);
        variantId = nullToEmpty(variantId);
        sourceKind = sourceKind != null ? sourceKind : "unknown";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Dimensions for aggregation key (e.g. policyVersion + variantId). */
    public List<String> dimensionValues() {
        return List.of(
                categoryKey,
                intentKey,
                actionKey,
                roleKey,
                policyVersion,
                experimentId,
                variantId,
                sourceKind,
                String.valueOf(fallbackApplied)
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String categoryKey = "";
        private String intentKey = "";
        private String actionKey = "";
        private String roleKey = "";
        private String policyVersion = "";
        private String experimentId = "";
        private String variantId = "";
        private String sourceKind = "unknown";
        private boolean fallbackApplied = false;

        public Builder categoryKey(String v) { this.categoryKey = v; return this; }
        public Builder intentKey(String v) { this.intentKey = v; return this; }
        public Builder actionKey(String v) { this.actionKey = v; return this; }
        public Builder roleKey(String v) { this.roleKey = v; return this; }
        public Builder policyVersion(String v) { this.policyVersion = v; return this; }
        public Builder experimentId(String v) { this.experimentId = v; return this; }
        public Builder variantId(String v) { this.variantId = v; return this; }
        public Builder sourceKind(String v) { this.sourceKind = v; return this; }
        public Builder fallbackApplied(boolean v) { this.fallbackApplied = v; return this; }

        public Builder fromOptionalStrings(Optional<String> experimentId, Optional<String> variantId) {
            this.experimentId = experimentId.orElse("");
            this.variantId = variantId.orElse("");
            return this;
        }

        public PolicyMetricsDimensions build() {
            return new PolicyMetricsDimensions(
                    categoryKey, intentKey, actionKey, roleKey,
                    policyVersion, experimentId, variantId, sourceKind, fallbackApplied
            );
        }
    }
}
