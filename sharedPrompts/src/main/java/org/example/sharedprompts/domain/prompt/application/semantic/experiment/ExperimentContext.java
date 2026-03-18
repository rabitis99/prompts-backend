package org.example.sharedprompts.domain.prompt.application.semantic.experiment;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Context for experiment routing: user, tenant, and optional request attributes.
 * Used by PolicySelectionStrategy and ExperimentPolicySelector for deterministic variant selection.
 */
public record ExperimentContext(
        Optional<String> userId,
        Optional<String> tenantId,
        Map<String, String> requestAttributes
) {
    public ExperimentContext {
        requestAttributes = requestAttributes != null ? Map.copyOf(requestAttributes) : Map.of();
    }

    public static ExperimentContext empty() {
        return new ExperimentContext(Optional.empty(), Optional.empty(), Map.of());
    }

    public static ExperimentContext of(String userId, String tenantId) {
        return new ExperimentContext(
                Optional.ofNullable(userId),
                Optional.ofNullable(tenantId),
                Map.of()
        );
    }

    public static ExperimentContext of(String userId, String tenantId, Map<String, String> requestAttributes) {
        return new ExperimentContext(
                Optional.ofNullable(userId),
                Optional.ofNullable(tenantId),
                requestAttributes != null ? requestAttributes : Collections.emptyMap()
        );
    }
}
