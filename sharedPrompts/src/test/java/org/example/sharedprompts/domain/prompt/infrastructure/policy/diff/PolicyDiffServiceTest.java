package org.example.sharedprompts.domain.prompt.infrastructure.policy.diff;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.PolicyDiff;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.RuleChange;
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
 * Policy document diff: structural comparison by rule identifier.
 */
@DisplayName("Policy diff service")
class PolicyDiffServiceTest {

    private DefaultPolicyDiffService diffService;
    private static final PolicyVersion V1 = PolicyVersion.of("v1", "v1", "test");
    private static final PolicyVersion V2 = PolicyVersion.of("v2", "v2", "test");

    @BeforeEach
    void setUp() {
        diffService = new DefaultPolicyDiffService();
    }

    @Test
    @DisplayName("two policy document sets diff correctly")
    void diffBetweenTwoBundles() {
        ValidatedPolicyBundle from = bundle(V1, Map.of("SUMMARY+EXTRACT", List.of("A", "B")));
        ValidatedPolicyBundle to = bundle(V2, Map.of("SUMMARY+EXTRACT", List.of("A", "B", "C"), "WRITING+GENERATE", List.of("X")));

        PolicyDiff diff = diffService.diff(from, to);

        assertThat(diff.fromVersion()).isEqualTo("v1");
        assertThat(diff.toVersion()).isEqualTo("v2");
        assertThat(diff.ruleChanges())
                .anyMatch(r -> r.changeType() == RuleChange.RuleChangeType.ADD && "WRITING+GENERATE".equals(r.ruleIdentifier()))
                .anyMatch(r -> r.changeType() == RuleChange.RuleChangeType.MODIFY && "SUMMARY+EXTRACT".equals(r.ruleIdentifier()));
        assertThat(diff.changeSummary()).contains("v1").contains("v2");
    }

    @Test
    @DisplayName("rule add is ADD, remove is REMOVE, value change is MODIFY")
    void addRemoveModifyClassified() {
        ValidatedPolicyBundle from = bundleWithRecommendation(V1, Map.of(
                "C1+I1", List.of("a1"),
                "C2+I2", List.of("b1", "b2")
        ));
        ValidatedPolicyBundle to = bundleWithRecommendation(V2, Map.of(
                "C1+I1", List.of("a1", "a2"),
                "C3+I3", List.of("c1")
        ));

        PolicyDiff diff = diffService.diff(from, to);

        List<RuleChange> add = diff.ruleChanges().stream().filter(r -> r.changeType() == RuleChange.RuleChangeType.ADD).toList();
        List<RuleChange> remove = diff.ruleChanges().stream().filter(r -> r.changeType() == RuleChange.RuleChangeType.REMOVE).toList();
        List<RuleChange> modify = diff.ruleChanges().stream().filter(r -> r.changeType() == RuleChange.RuleChangeType.MODIFY).toList();

        assertThat(add).hasSize(1).extracting(RuleChange::ruleIdentifier).containsExactly("C3+I3");
        assertThat(remove).hasSize(1).extracting(RuleChange::ruleIdentifier).containsExactly("C2+I2");
        assertThat(modify).hasSize(1).extracting(RuleChange::ruleIdentifier).containsExactly("C1+I1");
    }

    @Test
    @DisplayName("diff is stable by ruleIdentifier")
    void diffStableByRuleIdentifier() {
        ValidatedPolicyBundle from = bundleWithRecommendation(V1, Map.of("A+B", List.of("x", "y")));
        ValidatedPolicyBundle to = bundleWithRecommendation(V2, Map.of("A+B", List.of("y", "x")));

        PolicyDiff diff1 = diffService.diff(from, to);
        PolicyDiff diff2 = diffService.diff(from, to);

        assertThat(diff1.ruleChanges()).hasSize(1);
        assertThat(diff2.ruleChanges()).hasSize(1);
        assertThat(diff1.ruleChanges().get(0).ruleIdentifier()).isEqualTo("A+B");
        assertThat(diff2.ruleChanges().get(0).ruleIdentifier()).isEqualTo("A+B");
    }

    @Test
    @DisplayName("empty to empty yields empty diff")
    void emptyToEmpty() {
        ValidatedPolicyBundle empty = DefaultValidatedPolicyBundle.builder(V1).build();
        PolicyDiff diff = diffService.diff(empty, empty);
        assertThat(diff.isEmpty()).isTrue();
        assertThat(diff.ruleChanges()).isEmpty();
    }

    private static ValidatedPolicyBundle bundle(PolicyVersion version, Map<String, List<String>> recommendationRules) {
        return bundleWithRecommendation(version, recommendationRules);
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
}
