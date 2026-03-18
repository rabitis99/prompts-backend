package org.example.sharedprompts.domain.prompt.infrastructure.policy.repository;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.VersionedPolicyRepository;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.DefaultValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VersionedPolicyRepository: multiple versions, getLatest, getPrevious.
 */
@DisplayName("Versioned policy repository")
class VersionedPolicyRepositoryTest {

    @Test
    @DisplayName("manages multiple policy versions")
    void managesMultipleVersions() {
        ValidatedPolicyBundle b1 = bundle(PolicyVersion.of("v1", "v1", "test"), Map.of("k1", List.of("a")));
        ValidatedPolicyBundle b2 = bundle(PolicyVersion.of("v2", "v2", "test"), Map.of("k1", List.of("a", "b")));
        ValidatedPolicyBundle b3 = bundle(PolicyVersion.of("v3", "v3", "test"), Map.of("k1", List.of("a", "b", "c")));

        VersionedPolicyRepository repo = new InMemoryVersionedPolicyRepository(
                Map.of("v1", b1, "v2", b2, "v3", b3),
                List.of("v1", "v2", "v3")
        );

        assertThat(repo.listVersions()).containsExactly("v1", "v2", "v3");
        assertThat(repo.getPolicy("v1")).isPresent();
        assertThat(repo.getPolicy("v2")).isPresent();
        assertThat(repo.getPolicy("v3")).isPresent();
        assertThat(repo.getPolicy("v99")).isEmpty();
    }

    @Test
    @DisplayName("getLatest returns last in version order")
    void getLatest() {
        ValidatedPolicyBundle b1 = bundle(PolicyVersion.of("v1", "v1", "test"), Map.of());
        ValidatedPolicyBundle b2 = bundle(PolicyVersion.of("v2", "v2", "test"), Map.of());
        VersionedPolicyRepository repo = new InMemoryVersionedPolicyRepository(
                Map.of("v1", b1, "v2", b2),
                List.of("v1", "v2")
        );

        assertThat(repo.getLatest()).isPresent().get().extracting(b -> b.policyVersion().versionId()).isEqualTo("v2");
    }

    @Test
    @DisplayName("getPrevious returns previous version in order")
    void getPrevious() {
        ValidatedPolicyBundle b1 = bundle(PolicyVersion.of("v1", "v1", "test"), Map.of());
        ValidatedPolicyBundle b2 = bundle(PolicyVersion.of("v2", "v2", "test"), Map.of());
        ValidatedPolicyBundle b3 = bundle(PolicyVersion.of("v3", "v3", "test"), Map.of());
        VersionedPolicyRepository repo = new InMemoryVersionedPolicyRepository(
                Map.of("v1", b1, "v2", b2, "v3", b3),
                List.of("v1", "v2", "v3")
        );

        assertThat(repo.getPrevious("v1")).isEmpty();
        assertThat(repo.getPrevious("v2")).isPresent().get().extracting(b -> b.policyVersion().versionId()).isEqualTo("v1");
        assertThat(repo.getPrevious("v3")).isPresent().get().extracting(b -> b.policyVersion().versionId()).isEqualTo("v2");
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
