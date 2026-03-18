package org.example.sharedprompts.domain.prompt.infrastructure.policy.diff;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.PolicyChangeImpact;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.DefaultValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Policy change impact: SAFE / BEHAVIOR_CHANGE / BREAKING and affected scope.
 */
@DisplayName("Policy compatibility analyzer")
class PolicyCompatibilityAnalyzerTest {

    private DefaultPolicyCompatibilityAnalyzer analyzer;
    private static final PolicyVersion V1 = PolicyVersion.of("v1", "v1", "test");
    private static final PolicyVersion V2 = PolicyVersion.of("v2", "v2", "test");

    @BeforeEach
    void setUp() {
        analyzer = new DefaultPolicyCompatibilityAnalyzer(new DefaultPolicyDiffService());
    }

    @Test
    @DisplayName("preference reorder is BEHAVIOR_CHANGE")
    void preferenceReorderIsBehaviorChange() {
        ValidatedPolicyBundle oldB = bundleWithRecommendation(V1, Map.of("SUMMARY+EXTRACT", List.of("A", "B", "C")));
        ValidatedPolicyBundle newB = bundleWithRecommendation(V2, Map.of("SUMMARY+EXTRACT", List.of("C", "B", "A")));

        PolicyChangeImpact impact = analyzer.analyze(oldB, newB);

        assertThat(impact.breakingChanges()).isEmpty();
        assertThat(impact.behaviorChanges()).hasSize(1);
        assertThat(impact.behaviorChanges().get(0).policyFamily()).isEqualTo("RecommendationPreference");
    }

    @Test
    @DisplayName("compatibility rule removal is BREAKING")
    void compatibilityRemovalIsBreaking() {
        ValidatedPolicyBundle oldB = bundleWithCompatibility(V1, Map.of("SUMMARY+EXTRACT", List.of("g1", "g2")));
        ValidatedPolicyBundle newB = bundleWithCompatibility(V2, Map.of());

        PolicyChangeImpact impact = analyzer.analyze(oldB, newB);

        assertThat(impact.hasBreakingChanges()).isTrue();
        assertThat(impact.breakingChanges()).hasSize(1);
        assertThat(impact.breakingChanges().get(0).policyFamily()).isEqualTo("Compatibility");
    }

    @Test
    @DisplayName("preference add is SAFE")
    void preferenceAddIsSafe() {
        ValidatedPolicyBundle oldB = bundleWithRecommendation(V1, Map.of("C+I", List.of("a")));
        ValidatedPolicyBundle newB = bundleWithRecommendation(V2, Map.of("C+I", List.of("a"), "C2+I2", List.of("b")));

        PolicyChangeImpact impact = analyzer.analyze(oldB, newB);

        assertThat(impact.safeChanges()).anyMatch(r -> "C2+I2".equals(r.ruleIdentifier()));
        assertThat(impact.breakingChanges()).isEmpty();
    }

    @Test
    @DisplayName("impact returns affected categories, intents, actions")
    void impactReturnsAffectedScope() {
        ValidatedPolicyBundle oldB = bundleWithRecommendation(V1, Map.of("CAT1+INT1", List.of("act1")));
        ValidatedPolicyBundle newB = bundleWithRecommendation(V2, Map.of("CAT1+INT1", List.of("act1", "act2"), "CAT2+INT2", List.of("act3")));

        PolicyChangeImpact impact = analyzer.analyze(oldB, newB);

        assertThat(impact.affectedCategories()).contains("CAT1", "CAT2");
        assertThat(impact.affectedIntents()).contains("INT1", "INT2");
        assertThat(impact.affectedActions()).contains("act1", "act2", "act3");
    }

    private static ValidatedPolicyBundle bundleWithRecommendation(PolicyVersion version, Map<String, List<String>> rules) {
        RecommendationPreferencePolicyDocument doc = new RecommendationPreferencePolicyDocument(
                version.versionId(),
                RecommendationPreferencePolicyDocument.POLICY_TYPE,
                PolicyDocumentMetadata.empty(),
                Optional.empty(),
                rules
        );
        return DefaultValidatedPolicyBundle.builder(version).recommendationPreference(doc).build();
    }

    private static ValidatedPolicyBundle bundleWithCompatibility(PolicyVersion version, Map<String, List<String>> rules) {
        CompatibilityPolicyDocument doc = new CompatibilityPolicyDocument(
                version.versionId(),
                CompatibilityPolicyDocument.POLICY_TYPE,
                PolicyDocumentMetadata.empty(),
                Optional.empty(),
                rules
        );
        return DefaultValidatedPolicyBundle.builder(version).compatibility(doc).build();
    }
}
