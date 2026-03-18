package org.example.sharedprompts.domain.prompt.application.semantic.experiment;

import java.util.Optional;

/**
 * Determines experiment variant for a request (e.g. A/B bucket).
 * Must be deterministic: same context → same variant.
 */
@FunctionalInterface
public interface ExperimentPolicySelector {

    /**
     * Select variant identifier for the given context.
     * Same (userId, tenantId, attributes) must always yield the same variantId.
     */
    Optional<String> determineVariant(ExperimentContext context);
}
