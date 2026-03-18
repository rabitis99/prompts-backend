package org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;

import java.util.Optional;

/**
 * Default implementation of {@link ValidatedPolicyBundle}.
 */
public record DefaultValidatedPolicyBundle(
        PolicyVersion policyVersion,
        Optional<RecommendationPreferencePolicyDocument> recommendationPreference,
        Optional<CompatibilityPolicyDocument> compatibility,
        Optional<ObjectivePolicyDocument> objective,
        Optional<RolePreferencePolicyDocument> rolePreference,
        Optional<RoleCompatibilityPolicyDocument> roleCompatibility
) implements ValidatedPolicyBundle {

    public DefaultValidatedPolicyBundle {
        if (policyVersion == null) {
            throw new IllegalArgumentException("policyVersion is required");
        }
        recommendationPreference = recommendationPreference != null ? recommendationPreference : Optional.empty();
        compatibility = compatibility != null ? compatibility : Optional.empty();
        objective = objective != null ? objective : Optional.empty();
        rolePreference = rolePreference != null ? rolePreference : Optional.empty();
        roleCompatibility = roleCompatibility != null ? roleCompatibility : Optional.empty();
    }

    public static Builder builder(PolicyVersion policyVersion) {
        return new Builder(policyVersion);
    }

    public static final class Builder {
        private final PolicyVersion policyVersion;
        private RecommendationPreferencePolicyDocument recommendationPreference;
        private CompatibilityPolicyDocument compatibility;
        private ObjectivePolicyDocument objective;
        private RolePreferencePolicyDocument rolePreference;
        private RoleCompatibilityPolicyDocument roleCompatibility;

        private Builder(PolicyVersion policyVersion) {
            this.policyVersion = policyVersion;
        }

        public Builder recommendationPreference(RecommendationPreferencePolicyDocument doc) {
            this.recommendationPreference = doc;
            return this;
        }

        public Builder compatibility(CompatibilityPolicyDocument doc) {
            this.compatibility = doc;
            return this;
        }

        public Builder objective(ObjectivePolicyDocument doc) {
            this.objective = doc;
            return this;
        }

        public Builder rolePreference(RolePreferencePolicyDocument doc) {
            this.rolePreference = doc;
            return this;
        }

        public Builder roleCompatibility(RoleCompatibilityPolicyDocument doc) {
            this.roleCompatibility = doc;
            return this;
        }

        public DefaultValidatedPolicyBundle build() {
            return new DefaultValidatedPolicyBundle(
                    policyVersion,
                    Optional.ofNullable(recommendationPreference),
                    Optional.ofNullable(compatibility),
                    Optional.ofNullable(objective),
                    Optional.ofNullable(rolePreference),
                    Optional.ofNullable(roleCompatibility)
            );
        }
    }
}
