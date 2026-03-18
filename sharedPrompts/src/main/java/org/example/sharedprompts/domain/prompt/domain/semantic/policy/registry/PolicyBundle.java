package org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource;

import java.util.Objects;

/**
 * Bundle of policy sources for a single policy version.
 * Registry holds one bundle per version; services obtain sources via registry, not implementation type.
 */
public record PolicyBundle(
        ActionRecommendationPreferenceSource actionRecommendationPreferenceSource,
        RolePreferenceSource rolePreferenceSource,
        CompatibilityPolicySource compatibilityPolicySource,
        ObjectivePolicySource objectivePolicySource
) {
    public PolicyBundle {
        Objects.requireNonNull(actionRecommendationPreferenceSource, "actionRecommendationPreferenceSource");
        Objects.requireNonNull(rolePreferenceSource, "rolePreferenceSource");
        Objects.requireNonNull(compatibilityPolicySource, "compatibilityPolicySource");
        Objects.requireNonNull(objectivePolicySource, "objectivePolicySource");
    }
}
