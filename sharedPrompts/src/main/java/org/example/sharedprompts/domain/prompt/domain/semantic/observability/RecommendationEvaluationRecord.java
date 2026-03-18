package org.example.sharedprompts.domain.prompt.domain.semantic.observability;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Offline evaluation record: request context + recommended outcome + policy version + optional expected label.
 * Not used for online metrics; for precision/recall, human review, regression comparison.
 */
public record RecommendationEvaluationRecord(
        String categoryKey,
        String intentKey,
        List<String> recommendedActionKeys,
        String topActionKey,
        List<String> recommendedRoleKeys,
        String topRoleKey,
        String policyVersion,
        String experimentId,
        String variantId,
        Optional<String> expectedActionKey,
        Optional<String> expectedRoleKey,
        Optional<String> traceId,
        Optional<String> auditId,
        Optional<String> recommendationId
) {
    public RecommendationEvaluationRecord {
        recommendedActionKeys = recommendedActionKeys != null ? List.copyOf(recommendedActionKeys) : List.of();
        recommendedRoleKeys = recommendedRoleKeys != null ? List.copyOf(recommendedRoleKeys) : List.of();
        policyVersion = policyVersion != null ? policyVersion : "";
        experimentId = experimentId != null ? experimentId : "";
        variantId = variantId != null ? variantId : "";
        expectedActionKey = expectedActionKey != null ? expectedActionKey : Optional.empty();
        expectedRoleKey = expectedRoleKey != null ? expectedRoleKey : Optional.empty();
        traceId = traceId != null ? traceId : Optional.empty();
        auditId = auditId != null ? auditId : Optional.empty();
        recommendationId = recommendationId != null ? recommendationId : Optional.empty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String categoryKey = "";
        private String intentKey = "";
        private List<String> recommendedActionKeys = List.of();
        private String topActionKey = "";
        private List<String> recommendedRoleKeys = List.of();
        private String topRoleKey = "";
        private String policyVersion = "";
        private String experimentId = "";
        private String variantId = "";
        private Optional<String> expectedActionKey = Optional.empty();
        private Optional<String> expectedRoleKey = Optional.empty();
        private Optional<String> traceId = Optional.empty();
        private Optional<String> auditId = Optional.empty();
        private Optional<String> recommendationId = Optional.empty();

        public Builder categoryKey(String v) { this.categoryKey = v; return this; }
        public Builder intentKey(String v) { this.intentKey = v; return this; }
        public Builder recommendedActionKeys(List<String> v) { this.recommendedActionKeys = v != null ? List.copyOf(v) : List.of(); return this; }
        public Builder topActionKey(String v) { this.topActionKey = v; return this; }
        public Builder recommendedRoleKeys(List<String> v) { this.recommendedRoleKeys = v != null ? List.copyOf(v) : List.of(); return this; }
        public Builder topRoleKey(String v) { this.topRoleKey = v; return this; }
        public Builder policyVersion(String v) { this.policyVersion = v; return this; }
        public Builder experimentId(String v) { this.experimentId = v; return this; }
        public Builder variantId(String v) { this.variantId = v; return this; }
        public Builder expectedActionKey(String v) { this.expectedActionKey = Optional.ofNullable(v); return this; }
        public Builder expectedRoleKey(String v) { this.expectedRoleKey = Optional.ofNullable(v); return this; }
        public Builder traceId(String v) { this.traceId = Optional.ofNullable(v); return this; }
        public Builder auditId(String v) { this.auditId = Optional.ofNullable(v); return this; }
        public Builder recommendationId(String v) { this.recommendationId = Optional.ofNullable(v); return this; }

        public RecommendationEvaluationRecord build() {
            return new RecommendationEvaluationRecord(
                    categoryKey, intentKey, recommendedActionKeys, topActionKey,
                    recommendedRoleKeys, topRoleKey, policyVersion, experimentId, variantId,
                    expectedActionKey, expectedRoleKey, traceId, auditId, recommendationId
            );
        }
    }
}
