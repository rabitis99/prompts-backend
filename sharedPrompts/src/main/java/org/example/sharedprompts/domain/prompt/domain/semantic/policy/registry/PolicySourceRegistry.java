package org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.RoleRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;

/**
 * Registry of policy sources by version. Services depend on this interface, not on concrete source implementations.
 * Enables swapping in-memory / classpath JSON-YAML / future DB without changing recommendation logic.
 */
public interface PolicySourceRegistry {

    /**
     * Action recommendation preference source for the given version.
     */
    ActionRecommendationPreferenceSource getActionRecommendationPreferenceSource(PolicyVersion version);

    /**
     * Role preference source for the given version.
     */
    RolePreferenceSource getRolePreferenceSource(PolicyVersion version);

    /**
     * Compatibility policy source for the given version.
     */
    CompatibilityPolicySource getCompatibilityPolicySource(PolicyVersion version);

    /**
     * Objective policy source for the given version.
     */
    ObjectivePolicySource getObjectivePolicySource(PolicyVersion version);

    /**
     * Order policy built from the preference source for the given version.
     * Convenience so services do not construct policy from source.
     */
    ActionRecommendationOrderPolicy getActionRecommendationOrderPolicy(PolicyVersion version);

    /**
     * Role order policy built from the role preference source for the given version.
     * Role recommendation order is explicit and policy-driven, not profile list order.
     */
    RoleRecommendationOrderPolicy getRoleRecommendationOrderPolicy(PolicyVersion version);
}
