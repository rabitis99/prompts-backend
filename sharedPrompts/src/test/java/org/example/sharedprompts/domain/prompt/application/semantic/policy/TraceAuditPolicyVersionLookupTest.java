package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.DefaultValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTraceSnapshot;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.repository.InMemoryVersionedPolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trace/audit → policyVersionId → policy document lookup (VersionedPolicyRepository).
 */
@DisplayName("Trace/audit policy version lookup")
class TraceAuditPolicyVersionLookupTest {

    @Test
    @DisplayName("policy document can be retrieved by policyVersionId from trace snapshot")
    void documentRetrievedByPolicyVersionFromTrace() {
        String versionId = "2026-03-recommendation-v1";
        ValidatedPolicyBundle bundle = bundleForVersion(versionId, Map.of("SUMMARY+EXTRACT", List.of("A", "B")));
        VersionedPolicyRepository repo = new InMemoryVersionedPolicyRepository(
                Map.of(versionId, bundle),
                List.of(versionId)
        );

        String policyVersionIdFromTrace = "2026-03-recommendation-v1";
        Optional<ValidatedPolicyBundle> found = repo.getPolicy(policyVersionIdFromTrace);

        assertThat(found).isPresent();
        assertThat(found.get().policyVersion().versionId()).isEqualTo(policyVersionIdFromTrace);
        assertThat(found.get().recommendationPreference()).isPresent();
        assertThat(found.get().recommendationPreference().get().rules()).containsKey("SUMMARY+EXTRACT");
    }

    @Test
    @DisplayName("snapshot policyVersionId matches repository key for lookup")
    void snapshotPolicyVersionIdMatchesRepositoryKey() {
        RecommendationTraceSnapshot snapshot = new RecommendationTraceSnapshot(
                "SUMMARY", "EXTRACT", false,
                List.of(), List.of(), null, false,
                List.of("A", "B"), List.of(),
                "2026-03-recommendation-v1",
                "classpath:policy/v1",
                null, null
        );

        ValidatedPolicyBundle bundle = bundleForVersion(snapshot.policyVersionId(), Map.of());
        VersionedPolicyRepository repo = new InMemoryVersionedPolicyRepository(
                Map.of(snapshot.policyVersionId(), bundle),
                List.of(snapshot.policyVersionId())
        );

        Optional<ValidatedPolicyBundle> byVersion = repo.getPolicy(snapshot.policyVersionId());
        assertThat(byVersion).isPresent();
        assertThat(byVersion.get().policyVersion().versionId()).isEqualTo(snapshot.policyVersionId());
    }

    private static ValidatedPolicyBundle bundleForVersion(String versionId, Map<String, List<String>> rules) {
        PolicyVersion pv = PolicyVersion.of(versionId, "test", "test");
        RecommendationPreferencePolicyDocument doc = new RecommendationPreferencePolicyDocument(
                versionId,
                RecommendationPreferencePolicyDocument.POLICY_TYPE,
                PolicyDocumentMetadata.empty(),
                Optional.empty(),
                rules
        );
        return DefaultValidatedPolicyBundle.builder(pv).recommendationPreference(doc).build();
    }
}
