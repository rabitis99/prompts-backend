package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyBinder;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentLoader;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyLoadValidateResult;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.VersionedPolicyRepository;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.DefaultValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.DefaultPolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.PolicyDocumentPipeline;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.repository.InMemoryVersionedPolicyRepository;

import java.util.List;
import java.util.Map;

/**
 * Bootstrap: default policy from document pipeline (load → validate → bind) → repository + registry.
 * Falls back to empty validated bundle + bind when no classpath document exists.
 * Single source of truth for runtime policy; no hardcoded bundle in ResolutionConfig.
 */
public final class PolicyRuntimeBootstrap {

    private static final String DEFAULT_POLICY_CLASSPATH = "policy/default-recommendation.json";
    private static final String RECOMMENDATION_PREFERENCE_TYPE = "RecommendationPreference";

    private final VersionedPolicyRepository repository;
    private final PolicySourceRegistry registry;

    public PolicyRuntimeBootstrap(
            PolicyVersion defaultPolicyVersion,
            PolicyBinder policyBinder,
            PolicyDocumentPipeline pipeline,
            PolicyDocumentLoader loader
    ) {
        String versionId = defaultPolicyVersion.versionId();
        Map<String, ValidatedPolicyBundle> byVersion;
        List<String> versionOrder;
        Map<String, PolicyBundle> bundlesByVersion;

        Map<String, Object> raw = (loader != null && pipeline != null)
                ? loader.loadFromClasspath(DEFAULT_POLICY_CLASSPATH)
                : Map.of();
        if (pipeline != null && loader != null && raw != null && !raw.isEmpty()) {
            PolicyLoadValidateResult result = pipeline.loadValidateBundle(
                    defaultPolicyVersion,
                    Map.of(RECOMMENDATION_PREFERENCE_TYPE, raw)
            );
            if (result.isSuccess() && result.validatedBundle().isPresent()) {
                ValidatedPolicyBundle validated = result.validatedBundle().get();
                PolicyBundle bundle = policyBinder.bind(validated);
                byVersion = Map.of(versionId, validated);
                versionOrder = List.of(versionId);
                bundlesByVersion = Map.of(versionId, bundle);
            } else {
                byVersion = buildFallbackValidated(defaultPolicyVersion);
                versionOrder = List.of(versionId);
                bundlesByVersion = bindAndMap(versionId, policyBinder, byVersion);
            }
        } else {
            ValidatedPolicyBundle validated = DefaultValidatedPolicyBundle.builder(defaultPolicyVersion).build();
            PolicyBundle bundle = policyBinder.bind(validated);
            byVersion = Map.of(versionId, validated);
            versionOrder = List.of(versionId);
            bundlesByVersion = Map.of(versionId, bundle);
        }

        this.repository = new InMemoryVersionedPolicyRepository(byVersion, versionOrder);
        this.registry = new DefaultPolicySourceRegistry(bundlesByVersion);
    }

    private static Map<String, ValidatedPolicyBundle> buildFallbackValidated(PolicyVersion version) {
        ValidatedPolicyBundle validated = DefaultValidatedPolicyBundle.builder(version).build();
        return Map.of(version.versionId(), validated);
    }

    private static Map<String, PolicyBundle> bindAndMap(
            String versionId,
            PolicyBinder policyBinder,
            Map<String, ValidatedPolicyBundle> byVersion
    ) {
        ValidatedPolicyBundle v = byVersion.get(versionId);
        return Map.of(versionId, policyBinder.bind(v));
    }

    public VersionedPolicyRepository getRepository() {
        return repository;
    }

    public PolicySourceRegistry getRegistry() {
        return registry;
    }
}
