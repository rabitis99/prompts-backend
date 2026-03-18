package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import java.util.Optional;

/**
 * Policy context recorded in recommendation trace and audit.
 * Enables "which policy version produced this result?" without code inspection.
 */
public record PolicyTraceInfo(
        String policyVersionId,
        String policySourceId,
        Optional<String> experimentId,
        Optional<String> variantId
) {
    public PolicyTraceInfo {
        if (policyVersionId == null || policyVersionId.isBlank()) {
            throw new IllegalArgumentException("policyVersionId is required");
        }
        policySourceId = policySourceId != null ? policySourceId : policyVersionId;
        experimentId = experimentId != null ? experimentId : Optional.empty();
        variantId = variantId != null ? variantId : Optional.empty();
    }

    public static PolicyTraceInfo of(String policyVersionId, String policySourceId) {
        return new PolicyTraceInfo(
                policyVersionId,
                policySourceId != null ? policySourceId : policyVersionId,
                Optional.empty(),
                Optional.empty()
        );
    }

    public static PolicyTraceInfo of(
            String policyVersionId,
            String policySourceId,
            String experimentId,
            String variantId
    ) {
        return new PolicyTraceInfo(
                policyVersionId,
                policySourceId != null ? policySourceId : policyVersionId,
                Optional.ofNullable(experimentId),
                Optional.ofNullable(variantId)
        );
    }
}
