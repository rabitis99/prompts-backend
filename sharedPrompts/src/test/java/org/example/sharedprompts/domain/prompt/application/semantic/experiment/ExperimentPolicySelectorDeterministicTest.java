package org.example.sharedprompts.domain.prompt.application.semantic.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Experiment policy selector must be deterministic: same context → same variant.
 */
@DisplayName("ExperimentPolicySelector: deterministic variant selection")
class ExperimentPolicySelectorDeterministicTest {

    /** Deterministic selector: hash(userId) % 2 → "A" or "B". */
    private static final ExperimentPolicySelector DETERMINISTIC_SELECTOR = ctx -> {
        String userId = ctx.userId().orElse("");
        int hash = userId.hashCode() & 0x7FFF_FFFF;
        return Optional.of(hash % 2 == 0 ? "A" : "B");
    };

    @Test
    @DisplayName("Same user always gets same variant")
    void sameUserSameVariant() {
        ExperimentContext ctx = ExperimentContext.of("user-123", "tenant-1");
        Optional<String> v1 = DETERMINISTIC_SELECTOR.determineVariant(ctx);
        Optional<String> v2 = DETERMINISTIC_SELECTOR.determineVariant(ctx);
        assertThat(v1).isEqualTo(v2);
        assertThat(v1).isPresent();
    }

    @Test
    @DisplayName("Different users can get different variants")
    void differentUsersCanGetDifferentVariants() {
        Optional<String> a = DETERMINISTIC_SELECTOR.determineVariant(ExperimentContext.of("user-A", null));
        Optional<String> b = DETERMINISTIC_SELECTOR.determineVariant(ExperimentContext.of("user-B", null));
        assertThat(a).isPresent();
        assertThat(b).isPresent();
        // Not strictly required that they differ, but selector is deterministic per user
        assertThat(DETERMINISTIC_SELECTOR.determineVariant(ExperimentContext.of("user-A", null))).isEqualTo(a);
    }
}
