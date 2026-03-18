package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;

import java.util.List;
import java.util.Optional;

/**
 * Repository for versioned policy document sets.
 * Enables audit/trace → policyVersion → document lookup and reproducibility.
 */
public interface VersionedPolicyRepository {

    /**
     * Policy document bundle for the given version id, if present.
     */
    Optional<ValidatedPolicyBundle> getPolicy(String versionId);

    /**
     * All known version ids (e.g. from classpath or registry).
     */
    List<String> listVersions();

    /**
     * Latest version's document bundle, if any.
     */
    Optional<ValidatedPolicyBundle> getLatest();

    /**
     * Previous version's document bundle relative to the given version, if defined.
     */
    Optional<ValidatedPolicyBundle> getPrevious(String versionId);
}
