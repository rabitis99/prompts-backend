package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff.PolicyDiff;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.DefaultValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.diff.DefaultPolicyDiffService;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.repository.InMemoryVersionedPolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structure for reproducing recommendation with a previous policyVersion:
 * versioned repository + diff allows audit → version → document → analysis.
 */
@DisplayName("Policy version reproducibility structure")
class PolicyVersionReproducibilityTest {

    @Test
    @DisplayName("previous policyVersion document can be resolved and diffed for analysis")
    void previousVersionResolvedAndDiffed() {
        ValidatedPolicyBundle v1 = bundle(PolicyVersion.of("v1", "v1", "test"), Map.of("C+I", List.of("a", "b")));
        ValidatedPolicyBundle v2 = bundle(PolicyVersion.of("v2", "v2", "test"), Map.of("C+I", List.of("b", "a"), "C2+I2", List.of("c")));

        InMemoryVersionedPolicyRepository repo = new InMemoryVersionedPolicyRepository(
                Map.of("v1", v1, "v2", v2),
                List.of("v1", "v2")
        );
        DefaultPolicyDiffService diffService = new DefaultPolicyDiffService();

        Optional<ValidatedPolicyBundle> previous = repo.getPrevious("v2");
        assertThat(previous).isPresent();
        ValidatedPolicyBundle fromRepo = previous.get();
        assertThat(fromRepo.policyVersion().versionId()).isEqualTo("v1");

        Optional<ValidatedPolicyBundle> current = repo.getPolicy("v2");
        assertThat(current).isPresent();
        PolicyDiff diff = diffService.diff(fromRepo, current.get());
        assertThat(diff.fromVersion()).isEqualTo("v1");
        assertThat(diff.toVersion()).isEqualTo("v2");
        assertThat(diff.ruleChanges()).isNotEmpty();
    }

    private static ValidatedPolicyBundle bundle(PolicyVersion version, Map<String, List<String>> rules) {
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
