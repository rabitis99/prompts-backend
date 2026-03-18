package org.example.sharedprompts.domain.prompt.domain.semantic.policy.migration;

import java.util.List;
import java.util.Objects;

/**
 * Migration path between two policy versions.
 * Records steps (rename, split, merge, remove) for evolution and audit.
 */
public record PolicyMigration(
        String fromVersion,
        String toVersion,
        List<MigrationStep> migrationSteps
) {
    public PolicyMigration {
        Objects.requireNonNull(fromVersion, "fromVersion");
        Objects.requireNonNull(toVersion, "toVersion");
        migrationSteps = migrationSteps != null ? List.copyOf(migrationSteps) : List.of();
    }
}
