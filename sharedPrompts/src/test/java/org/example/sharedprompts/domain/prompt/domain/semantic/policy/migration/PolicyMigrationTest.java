package org.example.sharedprompts.domain.prompt.domain.semantic.policy.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Migration metadata links versions and preserves step identity.
 */
@DisplayName("Policy migration metadata")
class PolicyMigrationTest {

    @Test
    @DisplayName("migration metadata links fromVersion to toVersion")
    void migrationLinksVersions() {
        PolicyMigration m = new PolicyMigration(
                "v1",
                "v2",
                List.of(
                        MigrationStep.renameRule("oldKey", "newKey"),
                        MigrationStep.removeRule("deprecatedRule")
                )
        );

        assertThat(m.fromVersion()).isEqualTo("v1");
        assertThat(m.toVersion()).isEqualTo("v2");
        assertThat(m.migrationSteps()).hasSize(2);
        assertThat(m.migrationSteps().get(0).type()).isEqualTo(MigrationStepType.RENAME_RULE);
        assertThat(m.migrationSteps().get(0).targetIdentifier()).isEqualTo("oldKey");
        assertThat(m.migrationSteps().get(0).metadata()).containsEntry("newIdentifier", "newKey");
        assertThat(m.migrationSteps().get(1).type()).isEqualTo(MigrationStepType.REMOVE_RULE);
    }
}
