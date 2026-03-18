package org.example.sharedprompts.domain.prompt.domain.semantic.policy.schema;

import java.util.Optional;

/**
 * Metadata for a policy document (description, source type, experiment tag).
 */
public record PolicyDocumentMetadata(
        String description,
        String sourceType,
        Optional<String> experimentTag
) {
    public PolicyDocumentMetadata {
        description = description != null ? description : "";
        sourceType = sourceType != null ? sourceType : "unknown";
        experimentTag = experimentTag != null ? experimentTag : Optional.empty();
    }

    public static PolicyDocumentMetadata empty() {
        return new PolicyDocumentMetadata("", "unknown", Optional.empty());
    }
}
