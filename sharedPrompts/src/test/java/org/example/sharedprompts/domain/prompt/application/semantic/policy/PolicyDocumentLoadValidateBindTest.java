package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.PolicyDocumentPipeline;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.binding.DefaultPolicyBinder;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.loader.DefaultPolicyDocumentLoader;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.parser.*;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.validation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load → parse → validate → bind pipeline and validation separation.
 */
@DisplayName("Policy document load-validate-bind")
class PolicyDocumentLoadValidateBindTest {

    private PolicyDocumentPipeline pipeline;
    private PolicyValidationContext context;

    @BeforeEach
    void setUp() {
        org.example.sharedprompts.domain.prompt.common.enums.action.catalog.ActionTypeCatalog catalog =
                new org.example.sharedprompts.domain.prompt.common.enums.action.catalog.DefaultActionTypeCatalog();
        ActionTypeRegistry actionRegistry = new org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionTypeRegistry(
                catalog.getActionTypeEnumClasses()
        );
        CanonicalActionRegistry canonicalRegistry = new org.example.sharedprompts.domain.prompt.common.enums.action.canonical.DefaultCanonicalActionRegistry(actionRegistry);
        context = new DefaultPolicyValidationContext(actionRegistry, canonicalRegistry, null, null);

        PolicyDocumentLoader loader = new DefaultPolicyDocumentLoader(new ObjectMapper());
        List<PolicyDocumentParser> parsers = List.of(
                new RecommendationPreferencePolicyDocumentParser(),
                new CompatibilityPolicyDocumentParser(),
                new ObjectivePolicyDocumentParser(),
                new RolePreferencePolicyDocumentParser()
        );
        List<PolicyDocumentValidator> validators = List.of(
                new RecommendationPreferencePolicyDocumentValidator(context),
                new CompatibilityPolicyDocumentValidator(context),
                new ObjectivePolicyDocumentValidator(context),
                new RolePreferencePolicyDocumentValidator(context)
        );
        PolicyBinder binder = new DefaultPolicyBinder();
        pipeline = new PolicyDocumentPipeline(loader, parsers, validators, binder);
    }

    @Test
    @DisplayName("Valid recommendation document loads, validates, and binds to runtime bundle")
    void validRecommendationDocumentBinds() {
        Map<String, Object> raw = Map.of(
                "policyVersion", "test-v1",
                "policyType", "RecommendationPreference",
                "preferences", Map.of(
                        "WRITING+GENERATE", List.of("ACTION.WRITING.ARTICLE_WRITING", "ACTION.CREATIVE.CREATIVE_WRITING")
                )
        );
        PolicyVersion version = PolicyVersion.of("test-v1", "Test", "in-memory");
        PolicyLoadValidateBindResult result = pipeline.parseValidateBind(raw, "RecommendationPreference", version, java.util.Optional.empty());

        assertThat(result.validationResult().isBindable()).isTrue();
        assertThat(result.bundle()).isPresent();
        PolicyBundle bundle = result.bundle().get();
        assertThat(bundle.actionRecommendationPreferenceSource()).isNotNull();
        assertThat(bundle.rolePreferenceSource()).isNotNull();
        assertThat(bundle.compatibilityPolicySource()).isNotNull();
        assertThat(bundle.objectivePolicySource()).isNotNull();
    }

    @Test
    @DisplayName("Invalid document (unknown action key) fails semantic validation and does not bind")
    void invalidDocumentWithUnknownKeyDoesNotBind() {
        Map<String, Object> raw = Map.of(
                "policyVersion", "test-v1",
                "policyType", "RecommendationPreference",
                "preferences", Map.of(
                        "WRITING+GENERATE", List.of("ACTION.UNKNOWN.NOT_EXIST", "ACTION.WRITING.ARTICLE_WRITING")
                )
        );
        PolicyVersion version = PolicyVersion.of("test-v1", "Test", "in-memory");
        PolicyLoadValidateBindResult result = pipeline.parseValidateBind(raw, "RecommendationPreference", version, java.util.Optional.empty());

        assertThat(result.validationResult().isBindable()).isFalse();
        assertThat(result.bundle()).isEmpty();
        assertThat(result.validationResult().getErrors()).isNotEmpty();
        boolean hasUnknownKey = result.validationResult().getErrors().stream()
                .anyMatch(e -> "UNKNOWN_ACTION_KEY".equals(e.code()) && "ACTION.UNKNOWN.NOT_EXIST".equals(e.offendingValue()));
        assertThat(hasUnknownKey).isTrue();
    }

    @Test
    @DisplayName("Validation result contains code, message, path, offendingValue")
    void validationResultHasCodeMessagePathOffendingValue() {
        Map<String, Object> raw = Map.of(
                "policyVersion", "test-v1",
                "policyType", "RecommendationPreference",
                "preferences", Map.of(
                        "WRITING+GENERATE", List.of("ACTION.UNKNOWN.X")
                )
        );
        PolicyVersion version = PolicyVersion.of("test-v1", "Test", "in-memory");
        PolicyLoadValidateBindResult result = pipeline.parseValidateBind(raw, "RecommendationPreference", version, java.util.Optional.empty());

        assertThat(result.validationResult().getErrors()).isNotEmpty();
        var err = result.validationResult().getErrors().get(0);
        assertThat(err.code()).isNotBlank();
        assertThat(err.message()).isNotBlank();
        assertThat(err.path()).isNotNull();
        assertThat(err.offendingValueOptional()).isPresent();
    }

    @Test
    @DisplayName("Duplicate preference in same context yields warning; document still bindable")
    void duplicatePreferenceWarningStillBindable() {
        Map<String, Object> raw = Map.of(
                "policyVersion", "test-v1",
                "policyType", "RecommendationPreference",
                "preferences", Map.of(
                        "WRITING+GENERATE", List.of("ACTION.WRITING.ARTICLE_WRITING", "ACTION.WRITING.ARTICLE_WRITING", "ACTION.CREATIVE.CREATIVE_WRITING")
                )
        );
        PolicyVersion version = PolicyVersion.of("test-v1", "Test", "in-memory");
        PolicyLoadValidateBindResult result = pipeline.parseValidateBind(raw, "RecommendationPreference", version, java.util.Optional.empty());

        assertThat(result.validationResult().isBindable()).isTrue();
        assertThat(result.validationResult().getWarnings()).isNotEmpty();
        assertThat(result.bundle()).isPresent();
    }

    @Test
    @DisplayName("Missing policyVersion yields structural error and does not bind")
    void missingPolicyVersionStructuralErrorNoBind() {
        Map<String, Object> raw = Map.of(
                "policyType", "RecommendationPreference",
                "preferences", Map.of("WRITING+GENERATE", List.of("ACTION.WRITING.ARTICLE_WRITING"))
        );
        PolicyVersion version = PolicyVersion.of("test-v1", "Test", "in-memory");
        PolicyLoadValidateBindResult result = pipeline.parseValidateBind(raw, "RecommendationPreference", version, java.util.Optional.empty());

        assertThat(result.validationResult().isBindable()).isFalse();
        assertThat(result.validationResult().getErrors()).anyMatch(e -> "MISSING_FIELD".equals(e.code()) && "policyVersion".equals(e.path()));
        assertThat(result.bundle()).isEmpty();
    }

    @Test
    @DisplayName("Objective policy document validates and binds")
    void objectivePolicyDocumentValidatesAndBinds() {
        Map<String, Object> raw = Map.of(
                "policyVersion", "test-obj-v1",
                "policyType", "Objective",
                "explicitMappings", Map.of(
                        "ACTION.ANALYSIS.DATA_ANALYSIS", "ANALYTICAL",
                        "ACTION.CODING.CODE_REVIEW", "REASONING"
                ),
                "domainDefaults", Map.of(
                        "TECHNICAL", "REASONING",
                        "ANALYTICAL", "ANALYTICAL"
                )
        );
        PolicyVersion version = PolicyVersion.of("test-obj-v1", "Test", "in-memory");
        PolicyLoadValidateBindResult result = pipeline.parseValidateBind(raw, "Objective", version, java.util.Optional.empty());

        assertThat(result.validationResult().isBindable()).isTrue();
        assertThat(result.bundle()).isPresent();
        assertThat(result.bundle().get().objectivePolicySource()).isNotNull();
    }

    @Test
    @DisplayName("Compatibility policy document validates and binds")
    void compatibilityPolicyDocumentValidatesAndBinds() {
        Map<String, Object> raw = Map.of(
                "policyVersion", "test-compat-v1",
                "policyType", "Compatibility",
                "rules", Map.of(
                        "WRITING+GENERATE", List.of("LONG_FORM_WRITING", "CREATIVE_WRITING")
                )
        );
        PolicyVersion version = PolicyVersion.of("test-compat-v1", "Test", "in-memory");
        PolicyLoadValidateBindResult result = pipeline.parseValidateBind(raw, "Compatibility", version, java.util.Optional.empty());

        assertThat(result.validationResult().isBindable()).isTrue();
        assertThat(result.bundle()).isPresent();
        assertThat(result.bundle().get().compatibilityPolicySource()).isNotNull();
    }

    @Test
    @DisplayName("Role preference policy document validates and binds when no role keys to check")
    void rolePreferencePolicyDocumentValidatesAndBinds() {
        Map<String, Object> raw = Map.of(
                "policyVersion", "test-role-v1",
                "policyType", "RolePreference",
                "rules", Map.of(
                        "WRITING+GENERATE", List.of("ROLE.WRITING.EDITOR")
                )
        );
        PolicyVersion version = PolicyVersion.of("test-role-v1", "Test", "in-memory");
        PolicyLoadValidateBindResult result = pipeline.parseValidateBind(raw, "RolePreference", version, java.util.Optional.empty());

        assertThat(result.validationResult().isBindable()).isTrue();
        assertThat(result.bundle()).isPresent();
    }
}
