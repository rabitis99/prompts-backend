package org.example.sharedprompts.domain.prompt.infrastructure.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ActionRecommendationPreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultActionRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultRoleRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.RoleRecommendationOrderPolicy;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.role.RolePreferenceSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;

import java.util.Map;
import java.util.Objects;

/**
 * Registry that resolves policy sources by policy version id.
 * Builds order policy from preference source per version; no raw data in config.
 */
public final class DefaultPolicySourceRegistry implements PolicySourceRegistry {

    private final Map<String, PolicyBundle> bundlesByVersionId;

    public DefaultPolicySourceRegistry(Map<String, PolicyBundle> bundlesByVersionId) {
        this.bundlesByVersionId = Objects.requireNonNull(bundlesByVersionId, "bundlesByVersionId");
    }

    private PolicyBundle bundleFor(PolicyVersion version) {
        if (version == null || version.versionId() == null) {
            throw new IllegalArgumentException("PolicyVersion and versionId are required");
        }
        PolicyBundle bundle = bundlesByVersionId.get(version.versionId());
        if (bundle == null) {
            throw new IllegalArgumentException("No policy bundle for version: " + version.versionId());
        }
        return bundle;
    }

    @Override
    public ActionRecommendationPreferenceSource getActionRecommendationPreferenceSource(PolicyVersion version) {
        return bundleFor(version).actionRecommendationPreferenceSource();
    }

    @Override
    public RolePreferenceSource getRolePreferenceSource(PolicyVersion version) {
        return bundleFor(version).rolePreferenceSource();
    }

    @Override
    public CompatibilityPolicySource getCompatibilityPolicySource(PolicyVersion version) {
        return bundleFor(version).compatibilityPolicySource();
    }

    @Override
    public ObjectivePolicySource getObjectivePolicySource(PolicyVersion version) {
        return bundleFor(version).objectivePolicySource();
    }

    @Override
    public ActionRecommendationOrderPolicy getActionRecommendationOrderPolicy(PolicyVersion version) {
        ActionRecommendationPreferenceSource source = getActionRecommendationPreferenceSource(version);
        return new DefaultActionRecommendationOrderPolicy(source);
    }

    @Override
    public RoleRecommendationOrderPolicy getRoleRecommendationOrderPolicy(PolicyVersion version) {
        RolePreferenceSource source = getRolePreferenceSource(version);
        return new DefaultRoleRecommendationOrderPolicy(source);
    }
}
