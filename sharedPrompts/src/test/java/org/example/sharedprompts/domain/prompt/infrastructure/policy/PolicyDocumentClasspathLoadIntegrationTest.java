package org.example.sharedprompts.domain.prompt.infrastructure.policy;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyBinder;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentLoader;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentParser;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentValidator;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyLoadValidateBindResult;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.validation.DefaultPolicyValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load policy from classpath JSON and validate; policyVersion preserved through pipeline.
 */
@DisplayName("Classpath policy load and validation")
class PolicyDocumentClasspathLoadIntegrationTest {

    private PolicyDocumentPipeline pipeline;

    @BeforeEach
    void setUp() {
        org.example.sharedprompts.domain.prompt.common.enums.action.catalog.ActionTypeCatalog catalog =
                new org.example.sharedprompts.domain.prompt.common.enums.action.catalog.DefaultActionTypeCatalog();
        ActionTypeRegistry actionRegistry = new ActionTypeRegistry(catalog.getActionTypeEnumClasses());
        CanonicalActionRegistry canonicalRegistry = new org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry(actionRegistry);
        DefaultPolicyValidationContext context = new DefaultPolicyValidationContext(actionRegistry, canonicalRegistry, null, null);

        PolicyDocumentLoader loader = new org.example.sharedprompts.domain.prompt.infrastructure.policy.loader.DefaultPolicyDocumentLoader(
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        List<PolicyDocumentParser> parsers = List.of(
                new org.example.sharedprompts.domain.prompt.infrastructure.policy.parser.RecommendationPreferencePolicyDocumentParser(),
                new org.example.sharedprompts.domain.prompt.infrastructure.policy.parser.ObjectivePolicyDocumentParser()
        );
        List<PolicyDocumentValidator> validators = List.of(
                new org.example.sharedprompts.domain.prompt.infrastructure.policy.validation.RecommendationPreferencePolicyDocumentValidator(context),
                new org.example.sharedprompts.domain.prompt.infrastructure.policy.validation.ObjectivePolicyDocumentValidator(context)
        );
        PolicyBinder binder = new org.example.sharedprompts.domain.prompt.infrastructure.policy.binding.DefaultPolicyBinder();
        pipeline = new PolicyDocumentPipeline(loader, parsers, validators, binder);
    }

    @Test
    @DisplayName("Valid JSON from classpath loads, validates, and binds; policyVersion preserved")
    void validClasspathJsonLoadsValidatesBindsAndPreservesVersion() {
        PolicyVersion version = PolicyVersion.of("test-recommendation-v1", "Test classpath", "classpath-json");
        PolicyLoadValidateBindResult result = pipeline.loadValidateBind(
                "policy/recommendation-preferences-valid.json",
                "RecommendationPreference",
                version
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.validationResult().isBindable()).isTrue();
        assertThat(result.bundle()).isPresent();
        PolicyBundle bundle = result.bundle().get();
        assertThat(bundle.actionRecommendationPreferenceSource()).isNotNull();
        assertThat(result.policyVersion()).isEqualTo(version);
    }

    @Test
    @DisplayName("Invalid JSON (unknown action key) from classpath fails semantic validation and does not bind")
    void invalidClasspathJsonFailsSemanticValidationNoBind() {
        PolicyVersion version = PolicyVersion.of("test-v1", "Test", "classpath-json");
        PolicyLoadValidateBindResult result = pipeline.loadValidateBind(
                "policy/recommendation-preferences-invalid-key.json",
                "RecommendationPreference",
                version
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.validationResult().isBindable()).isFalse();
        assertThat(result.bundle()).isEmpty();
        assertThat(result.validationResult().getErrors()).anyMatch(e -> "UNKNOWN_ACTION_KEY".equals(e.code()));
    }

    @Test
    @DisplayName("Objective JSON from classpath loads and validates")
    void objectiveClasspathJsonLoadsAndValidates() {
        PolicyVersion version = PolicyVersion.of("test-objective-v1", "Test", "classpath-json");
        PolicyLoadValidateBindResult result = pipeline.loadValidateBind(
                "policy/objective-valid.json",
                "Objective",
                version
        );

        assertThat(result.validationResult().isBindable()).isTrue();
        assertThat(result.bundle()).isPresent();
    }
}
