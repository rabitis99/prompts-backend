package org.example.sharedprompts.domain.prompt.infrastructure.policy.validation;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyValidationContext;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.RecommendationPreferencePolicyDocument;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural and semantic validation are separate: structural (format, required fields) runs first;
 * semantic (stable key existence) runs only when structure is valid.
 */
@DisplayName("Structural vs semantic validation separation")
class StructuralVsSemanticValidationTest {

    private static final PolicyValidationContext EMPTY_CONTEXT = new PolicyValidationContext() {
        @Override public Set<String> knownCategoryKeys() { return Set.of("WRITING", "DEVELOPMENT"); }
        @Override public Set<String> knownIntentKeys() { return Set.of("GENERATE", "EXPLAIN"); }
        @Override public Set<String> knownActionKeys() { return Set.of("ACTION.WRITING.ARTICLE_WRITING"); }
        @Override public Set<String> knownActionGroupKeys() { return Set.of(); }
        @Override public Set<String> knownRoleKeys() { return Set.of(); }
        @Override public Set<String> knownObjectiveNames() { return Set.of(); }
        @Override public Set<String> knownTaskDomainNames() { return Set.of(); }
    };

    @Test
    @DisplayName("Structural error (missing policyVersion) is reported before semantic checks")
    void structuralErrorReportedFirst() {
        RecommendationPreferencePolicyDocument doc = new RecommendationPreferencePolicyDocument(
                null,
                "RecommendationPreference",
                null,
                java.util.Optional.empty(),
                Map.of("WRITING+GENERATE", List.of("ACTION.UNKNOWN.X"))
        );
        RecommendationPreferencePolicyDocumentValidator validator =
                new RecommendationPreferencePolicyDocumentValidator(EMPTY_CONTEXT);
        PolicyValidationResult result = validator.validate(doc);

        assertThat(result.isBindable()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> "MISSING_FIELD".equals(e.code()) && "policyVersion".equals(e.path()));
    }

    @Test
    @DisplayName("When structure is valid, semantic error (unknown action key) is reported")
    void semanticErrorWhenStructureValid() {
        RecommendationPreferencePolicyDocument doc = new RecommendationPreferencePolicyDocument(
                "v1",
                "RecommendationPreference",
                null,
                java.util.Optional.empty(),
                Map.of("WRITING+GENERATE", List.of("ACTION.UNKNOWN.NOT_EXIST"))
        );
        RecommendationPreferencePolicyDocumentValidator validator =
                new RecommendationPreferencePolicyDocumentValidator(EMPTY_CONTEXT);
        PolicyValidationResult result = validator.validate(doc);

        assertThat(result.isBindable()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> "UNKNOWN_ACTION_KEY".equals(e.code()));
        assertThat(result.getErrors()).noneMatch(e -> "MISSING_FIELD".equals(e.code()));
    }

    @Test
    @DisplayName("Valid structure and known keys pass validation")
    void validStructureAndKnownKeysPass() {
        RecommendationPreferencePolicyDocument doc = new RecommendationPreferencePolicyDocument(
                "v1",
                "RecommendationPreference",
                null,
                java.util.Optional.empty(),
                Map.of("WRITING+GENERATE", List.of("ACTION.WRITING.ARTICLE_WRITING"))
        );
        RecommendationPreferencePolicyDocumentValidator validator =
                new RecommendationPreferencePolicyDocumentValidator(EMPTY_CONTEXT);
        PolicyValidationResult result = validator.validate(doc);

        assertThat(result.isBindable()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }
}
