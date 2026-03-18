package org.example.sharedprompts.domain.prompt.infrastructure.policy.binding;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyBinder;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultActionRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;

/**
 * Binds a {@link ValidatedPolicyBundle} to runtime {@link PolicyBundle}.
 * Only call when bundle is valid (no validation errors).
 */
public final class DefaultPolicyBinder implements PolicyBinder {

    private final RecommendationPreferencePolicyBinder recommendationBinder = new RecommendationPreferencePolicyBinder();
    private final CompatibilityPolicyBinder compatibilityBinder = new CompatibilityPolicyBinder();
    private final ObjectivePolicyBinder objectiveBinder = new ObjectivePolicyBinder();
    private final RolePreferencePolicyBinder rolePreferenceBinder = new RolePreferencePolicyBinder();

    @Override
    public PolicyBundle bind(ValidatedPolicyBundle validated) {
        if (validated == null) {
            throw new IllegalArgumentException("ValidatedPolicyBundle is required");
        }
        ActionRecommendationPreferenceSource actionPref = validated.recommendationPreference()
                .map(recommendationBinder::bind)
                .orElseGet(() -> new org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultActionRecommendationPreferenceSource());
        RolePreferenceSource rolePref = validated.rolePreference()
                .map(rolePreferenceBinder::bind)
                .orElseGet(() -> new org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.DefaultRolePreferenceSource());
        CompatibilityPolicySource compat = validated.compatibility()
                .map(compatibilityBinder::bind)
                .orElseGet(() -> new org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.DefaultCompatibilityPolicySource());
        ObjectivePolicySource objective = validated.objective()
                .map(objectiveBinder::bind)
                .orElseGet(org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.DefaultObjectivePolicySource::new);

        return new PolicyBundle(
                actionPref,
                rolePref,
                compat,
                objective
        );
    }
}
