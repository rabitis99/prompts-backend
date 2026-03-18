package org.example.sharedprompts.domain.prompt.application.semantic.recommendation;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.DefaultPolicySelectionStrategy;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicySelectionStrategy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.DefaultCompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.DefaultObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.DefaultRolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.DefaultPolicySourceRegistry;

import java.util.Map;

/** Test fixtures for policy version / registry so tests can build SemanticRecommendationService without Spring. */
public final class PolicyTestFixtures {

    public static final String TEST_POLICY_VERSION_ID = "test-recommendation-v1";

    public static PolicyVersion testPolicyVersion() {
        return PolicyVersion.of(TEST_POLICY_VERSION_ID, "Test in-memory", "in-memory");
    }

    public static PolicySourceRegistry registryWithSingleVersion(ActionRecommendationPreferenceSource actionPref) {
        return registryWithSingleVersion(actionPref, new DefaultRolePreferenceSource());
    }

    /** Registry with custom action and role preference sources for role-policy tests. */
    public static PolicySourceRegistry registryWithSingleVersion(
            ActionRecommendationPreferenceSource actionPref,
            RolePreferenceSource rolePref) {
        PolicyVersion version = testPolicyVersion();
        PolicyBundle bundle = new PolicyBundle(
                actionPref != null ? actionPref : new DefaultActionRecommendationPreferenceSource(Map.of()),
                rolePref != null ? rolePref : new DefaultRolePreferenceSource(),
                new DefaultCompatibilityPolicySource(),
                new DefaultObjectivePolicySource()
        );
        return new DefaultPolicySourceRegistry(Map.of(version.versionId(), bundle));
    }

    public static PolicySelectionStrategy singleVersionStrategy() {
        return new DefaultPolicySelectionStrategy(testPolicyVersion());
    }
}
