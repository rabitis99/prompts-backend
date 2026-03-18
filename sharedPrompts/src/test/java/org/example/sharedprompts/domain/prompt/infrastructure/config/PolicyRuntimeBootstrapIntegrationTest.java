package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyBinder;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.VersionedPolicyRepository;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.binding.DefaultPolicyBinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runtime registry and repository are built from pipeline/binder bootstrap, not hardcoded bundle.
 * Unit test: bootstrap uses PolicyBinder and default ValidatedPolicyBundle, not config-instantiated sources.
 */
@DisplayName("Policy runtime bootstrap integration")
class PolicyRuntimeBootstrapIntegrationTest {

    private static final String DEFAULT_POLICY_VERSION_ID = "2026-03-recommendation-v1";

    @Test
    @DisplayName("repository contains default policy version from bootstrap")
    void repositoryContainsDefaultVersion() {
        PolicyVersion defaultPolicyVersion = PolicyVersion.of(DEFAULT_POLICY_VERSION_ID, "Default", "in-memory");
        PolicyBinder policyBinder = new DefaultPolicyBinder();
        PolicyRuntimeBootstrap bootstrap = new PolicyRuntimeBootstrap(
                defaultPolicyVersion, policyBinder, null, null);
        VersionedPolicyRepository repository = bootstrap.getRepository();

        assertThat(repository.listVersions()).containsExactly(DEFAULT_POLICY_VERSION_ID);
        Optional<?> latest = repository.getLatest();
        assertThat(latest).isPresent();
        assertThat(repository.getPolicy(DEFAULT_POLICY_VERSION_ID)).isPresent();
    }

    @Test
    @DisplayName("registry resolves bundle for default version")
    void registryResolvesBundleForDefaultVersion() {
        PolicyVersion defaultPolicyVersion = PolicyVersion.of(DEFAULT_POLICY_VERSION_ID, "Default", "in-memory");
        PolicyBinder policyBinder = new DefaultPolicyBinder();
        PolicyRuntimeBootstrap bootstrap = new PolicyRuntimeBootstrap(
                defaultPolicyVersion, policyBinder, null, null);
        PolicySourceRegistry registry = bootstrap.getRegistry();

        registry.getActionRecommendationPreferenceSource(defaultPolicyVersion);
        registry.getRolePreferenceSource(defaultPolicyVersion);
        registry.getCompatibilityPolicySource(defaultPolicyVersion);
        registry.getObjectivePolicySource(defaultPolicyVersion);
    }

    @Test
    @DisplayName("trace policyVersion id matches repository key")
    void tracePolicyVersionMatchesRepositoryKey() {
        PolicyVersion defaultPolicyVersion = PolicyVersion.of(DEFAULT_POLICY_VERSION_ID, "Default", "in-memory");
        PolicyBinder policyBinder = new DefaultPolicyBinder();
        PolicyRuntimeBootstrap bootstrap = new PolicyRuntimeBootstrap(
                defaultPolicyVersion, policyBinder, null, null);
        VersionedPolicyRepository repository = bootstrap.getRepository();

        String versionId = defaultPolicyVersion.versionId();
        assertThat(repository.getPolicy(versionId)).isPresent();
        assertThat(repository.getPolicy(versionId).get().policyVersion().versionId()).isEqualTo(versionId);
    }
}
