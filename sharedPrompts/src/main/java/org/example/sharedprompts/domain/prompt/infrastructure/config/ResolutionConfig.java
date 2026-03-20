package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.DefaultPolicySelectionStrategy;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicySelectionStrategy;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionDomainRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolver;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility.CompatibilityPolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective.ObjectivePolicySource;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.ConcreteActionByGroupIndex;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultConcreteActionByGroupIndex;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.DefaultRecommendationConcreteActionExpander;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation.RecommendationConcreteActionExpander;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DomainResolverPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.DefaultObjectiveHeuristicInferencePolicy;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistry;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveMappingRegistryPort;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveHeuristicInferencePolicy;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolver;
import org.example.sharedprompts.domain.prompt.domain.resolutions.ObjectiveResolverPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TaskDomain / PromptObjective 해석 빈 설정.
 * Policy version and registry: policy data lives in sources, not in config; version is first-class for trace/audit.
 */
@Configuration
public class ResolutionConfig {

    private static final String DEFAULT_POLICY_VERSION_ID = "2026-03-recommendation-v1";

    @Bean
    public DomainResolverPort domainResolver(ActionDomainRegistry actionDomainRegistry) {
        return new DomainResolver(actionDomainRegistry);
    }

    @Bean
    public PolicyVersion defaultPolicyVersion() {
        return PolicyVersion.of(DEFAULT_POLICY_VERSION_ID, "Default in-memory", "in-memory");
    }

    @Bean
    public PolicySelectionStrategy policySelectionStrategy(PolicyVersion defaultPolicyVersion) {
        return new DefaultPolicySelectionStrategy(defaultPolicyVersion);
    }

    @Bean
    public ObjectivePolicySource objectivePolicySource(
            PolicySourceRegistry policySourceRegistry,
            PolicyVersion defaultPolicyVersion) {
        return policySourceRegistry.getObjectivePolicySource(defaultPolicyVersion);
    }

    @Bean
    public ObjectiveHeuristicInferencePolicy objectiveHeuristicInferencePolicy() {
        return new DefaultObjectiveHeuristicInferencePolicy();
    }

    @Bean
    public ObjectiveMappingRegistryPort objectiveMappingRegistry(
            ObjectivePolicySource objectivePolicySource,
            ObjectiveHeuristicInferencePolicy heuristicInferencePolicy) {
        return new ObjectiveMappingRegistry(objectivePolicySource, heuristicInferencePolicy);
    }

    @Bean
    public ObjectiveResolverPort objectiveResolver(ObjectiveMappingRegistryPort mappingRegistry) {
        return new ObjectiveResolver(mappingRegistry);
    }

    @Bean
    public CategorySemanticProfileSeedSource categorySemanticProfileSeedSource() {
        return new DefaultCategorySemanticProfileSeedSource();
    }

    /** Profile registry: assembles from seed source; optional CompatibilityPolicySource from registry. */
    @Bean
    public CategorySemanticProfileRegistry categorySemanticProfileRegistry(
            CanonicalActionRegistry canonicalActionRegistry,
            PolicySourceRegistry policySourceRegistry,
            PolicyVersion defaultPolicyVersion,
            CategorySemanticProfileSeedSource categorySemanticProfileSeedSource) {
        CompatibilityPolicySource compat = policySourceRegistry.getCompatibilityPolicySource(defaultPolicyVersion);
        return new DefaultCategorySemanticProfileRegistry(canonicalActionRegistry, compat, categorySemanticProfileSeedSource);
    }

    // --- Recommendation: concrete expansion; ordering comes from registry per request version ---

    @Bean
    public ConcreteActionByGroupIndex concreteActionByGroupIndex(ActionTypeRegistry actionTypeRegistry) {
        return new DefaultConcreteActionByGroupIndex(actionTypeRegistry.getAll());
    }

    @Bean
    public RecommendationConcreteActionExpander recommendationConcreteActionExpander(
            ConcreteActionByGroupIndex concreteActionByGroupIndex) {
        return new DefaultRecommendationConcreteActionExpander(concreteActionByGroupIndex);
    }
}
