package org.example.sharedprompts.domain.prompt.domain.semantic.policy.migration;

import java.util.Map;
import java.util.Objects;

/**
 * Single step in a policy version migration path.
 */
public record MigrationStep(
        MigrationStepType type,
        String targetIdentifier,
        Map<String, Object> metadata
) {
    public MigrationStep {
        Objects.requireNonNull(type, "type");
        if (targetIdentifier == null || targetIdentifier.isBlank()) {
            throw new IllegalArgumentException("targetIdentifier is required");
        }
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static MigrationStep renameRule(String targetIdentifier, String newIdentifier) {
        return new MigrationStep(
                MigrationStepType.RENAME_RULE,
                targetIdentifier,
                Map.of("newIdentifier", newIdentifier)
        );
    }

    public static MigrationStep removeRule(String targetIdentifier) {
        return new MigrationStep(MigrationStepType.REMOVE_RULE, targetIdentifier, Map.of());
    }
}
