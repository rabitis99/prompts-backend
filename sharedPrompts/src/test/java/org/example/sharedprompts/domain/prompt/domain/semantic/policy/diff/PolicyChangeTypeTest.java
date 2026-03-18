package org.example.sharedprompts.domain.prompt.domain.semantic.policy.diff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Policy change type classification (SAFE / BEHAVIOR_CHANGE / BREAKING).
 */
@DisplayName("Policy change type")
class PolicyChangeTypeTest {

    @Test
    @DisplayName("all three change types exist")
    void allChangeTypesExist() {
        assertThat(PolicyChangeType.SAFE).isNotNull();
        assertThat(PolicyChangeType.BEHAVIOR_CHANGE).isNotNull();
        assertThat(PolicyChangeType.BREAKING).isNotNull();
    }
}
