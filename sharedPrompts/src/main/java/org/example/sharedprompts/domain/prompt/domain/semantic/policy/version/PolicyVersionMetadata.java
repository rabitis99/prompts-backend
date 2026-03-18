package org.example.sharedprompts.domain.prompt.domain.semantic.policy.version;

import java.time.Instant;
import java.util.Optional;

/**
 * Metadata for a policy version (display/audit); version identity is {@link PolicyVersion}.
 */
public record PolicyVersionMetadata(
        String versionId,
        Instant createdAt,
        String description,
        String sourceType,
        Optional<String> experimentTag
) {
    public static PolicyVersionMetadata from(PolicyVersion version) {
        if (version == null) return null;
        return new PolicyVersionMetadata(
                version.versionId(),
                version.createdAt(),
                version.description(),
                version.sourceType(),
                version.experimentTag()
        );
    }
}
