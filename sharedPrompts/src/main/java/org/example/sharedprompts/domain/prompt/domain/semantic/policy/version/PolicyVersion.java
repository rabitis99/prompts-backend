package org.example.sharedprompts.domain.prompt.domain.semantic.policy.version;

import java.time.Instant;
import java.util.Optional;

/**
 * First-class policy version identifier for recommendation engine.
 * Used in trace, audit, experiment, and debug; not a simple string.
 */
public record PolicyVersion(
        String versionId,
        Instant createdAt,
        String description,
        String sourceType,
        Optional<String> experimentTag
) {
    public PolicyVersion {
        if (versionId == null || versionId.isBlank()) {
            throw new IllegalArgumentException("versionId is required");
        }
        description = description != null ? description : "";
        sourceType = sourceType != null ? sourceType : "in-memory";
        experimentTag = experimentTag != null ? experimentTag : Optional.empty();
    }

    public static PolicyVersion of(String versionId, String description, String sourceType) {
        return new PolicyVersion(versionId, Instant.now(), description, sourceType, Optional.empty());
    }

    public static PolicyVersion of(String versionId, String description, String sourceType, String experimentTag) {
        return new PolicyVersion(
                versionId,
                Instant.now(),
                description,
                sourceType,
                Optional.ofNullable(experimentTag)
        );
    }
}
