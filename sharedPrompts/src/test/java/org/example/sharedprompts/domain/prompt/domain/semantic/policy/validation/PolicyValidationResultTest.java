package org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation result model: code, message, path, offendingValue; error vs warning; bindable only when no errors.
 */
class PolicyValidationResultTest {

    @Test
    void successHasNoErrorsAndIsBindable() {
        PolicyValidationResult r = PolicyValidationResult.success(List.of());
        assertThat(r.valid()).isTrue();
        assertThat(r.errors()).isEmpty();
        assertThat(r.isBindable()).isTrue();
    }

    @Test
    void successWithWarningsIsStillBindable() {
        PolicyValidationWarning w = PolicyValidationWarning.of("W001", "Duplicate entry");
        PolicyValidationResult r = PolicyValidationResult.success(List.of(w));
        assertThat(r.valid()).isTrue();
        assertThat(r.errors()).isEmpty();
        assertThat(r.warnings()).hasSize(1);
        assertThat(r.warnings().get(0).code()).isEqualTo("W001");
        assertThat(r.warnings().get(0).message()).isEqualTo("Duplicate entry");
        assertThat(r.isBindable()).isTrue();
    }

    @Test
    void failureWithErrorsIsNotBindable() {
        PolicyValidationError e = new PolicyValidationError("E001", "Unknown action key", "rules.WRITING+GENERATE", "ACTION.UNKNOWN.X");
        PolicyValidationResult r = PolicyValidationResult.failure(List.of(e));
        assertThat(r.valid()).isFalse();
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().get(0).code()).isEqualTo("E001");
        assertThat(r.errors().get(0).message()).isEqualTo("Unknown action key");
        assertThat(r.errors().get(0).path()).isEqualTo("rules.WRITING+GENERATE");
        assertThat(r.errors().get(0).offendingValue()).isEqualTo("ACTION.UNKNOWN.X");
        assertThat(r.isBindable()).isFalse();
    }

    @Test
    void failureWithErrorsAndWarningsIsNotBindable() {
        PolicyValidationError e = new PolicyValidationError("E002", "Missing policyVersion", "policyVersion", null);
        PolicyValidationWarning w = PolicyValidationWarning.of("W002", "Unused entry");
        PolicyValidationResult r = PolicyValidationResult.failure(List.of(e), List.of(w));
        assertThat(r.valid()).isFalse();
        assertThat(r.getErrors()).hasSize(1);
        assertThat(r.getWarnings()).hasSize(1);
        assertThat(r.isBindable()).isFalse();
    }
}
