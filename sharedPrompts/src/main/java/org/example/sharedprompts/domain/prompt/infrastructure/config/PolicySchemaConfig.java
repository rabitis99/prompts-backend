package org.example.sharedprompts.domain.prompt.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyBinder;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyCompatibilityAnalyzer;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDiffService;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentLoader;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentParser;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentValidator;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyValidationContext;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.PolicyDocumentPipeline;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.binding.DefaultPolicyBinder;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.diff.DefaultPolicyCompatibilityAnalyzer;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.diff.DefaultPolicyDiffService;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.loader.DefaultPolicyDocumentLoader;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.parser.*;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.validation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Policy schema/DSL infrastructure: loader, parsers, validators, binder, pipeline.
 * VersionedPolicyRepository and PolicySourceRegistry are wired from PolicyBootstrapConfig (pipeline/binder → bootstrap).
 */
@Configuration
public class PolicySchemaConfig {

    @Bean
    public PolicyDocumentLoader policyDocumentLoader(ObjectMapper objectMapper) {
        return new DefaultPolicyDocumentLoader(objectMapper);
    }

    @Bean
    public List<PolicyDocumentParser> policyDocumentParsers() {
        return List.of(
                new RecommendationPreferencePolicyDocumentParser(),
                new CompatibilityPolicyDocumentParser(),
                new ObjectivePolicyDocumentParser(),
                new RolePreferencePolicyDocumentParser(),
                new RoleCompatibilityPolicyDocumentParser()
        );
    }

    @Bean
    public PolicyValidationContext policyValidationContext(
            ActionTypeRegistry actionTypeRegistry,
            CanonicalActionRegistry canonicalActionRegistry
    ) {
        return new DefaultPolicyValidationContext(
                actionTypeRegistry,
                canonicalActionRegistry,
                null,
                null
        );
    }

    @Bean
    public List<PolicyDocumentValidator> policyDocumentValidators(PolicyValidationContext policyValidationContext) {
        return List.of(
                new RecommendationPreferencePolicyDocumentValidator(policyValidationContext),
                new CompatibilityPolicyDocumentValidator(policyValidationContext),
                new ObjectivePolicyDocumentValidator(policyValidationContext),
                new RolePreferencePolicyDocumentValidator(policyValidationContext)
        );
    }

    @Bean
    public PolicyBinder policyBinder() {
        return new DefaultPolicyBinder();
    }

    @Bean
    public PolicyDocumentPipeline policyDocumentPipeline(
            PolicyDocumentLoader policyDocumentLoader,
            List<PolicyDocumentParser> policyDocumentParsers,
            List<PolicyDocumentValidator> policyDocumentValidators,
            PolicyBinder policyBinder
    ) {
        return new PolicyDocumentPipeline(
                policyDocumentLoader,
                policyDocumentParsers,
                policyDocumentValidators,
                policyBinder
        );
    }

    @Bean
    public PolicyDiffService policyDiffService() {
        return new DefaultPolicyDiffService();
    }

    @Bean
    public PolicyCompatibilityAnalyzer policyCompatibilityAnalyzer(PolicyDiffService policyDiffService) {
        return new DefaultPolicyCompatibilityAnalyzer(policyDiffService);
    }
}
