package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.application.semantic.experiment.ExperimentContext;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Policy selection strategy selects policy version from request context.
 */
@DisplayName("PolicySelectionStrategy: version selection from context")
class PolicySelectionStrategyTest {

    @Test
    @DisplayName("DefaultPolicySelectionStrategy returns same version for any context")
    void defaultStrategyReturnsActiveVersion() {
        PolicyVersion version = PolicyVersion.of("v1", "Default", "in-memory");
        PolicySelectionStrategy strategy = new DefaultPolicySelectionStrategy(version);

        assertThat(strategy.selectPolicyVersion(ExperimentContext.empty()).versionId()).isEqualTo("v1");
        assertThat(strategy.selectPolicyVersion(ExperimentContext.of("user1", "tenant1")).versionId()).isEqualTo("v1");
    }
}
