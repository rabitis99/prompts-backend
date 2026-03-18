package org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation;

import java.util.Collections;
import java.util.List;

/**
 * Result of policy document validation. Valid only when errors is empty.
 * Warnings do not block binding; errors do.
 */
public record PolicyValidationResult(
        boolean valid,
        List<PolicyValidationError> errors,
        List<PolicyValidationWarning> warnings
) {
    public PolicyValidationResult {
        errors = errors != null ? List.copyOf(errors) : List.of();
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
        valid = errors.isEmpty();
    }

    public static PolicyValidationResult success(List<PolicyValidationWarning> warnings) {
        return new PolicyValidationResult(true, List.of(), warnings != null ? warnings : List.of());
    }

    public static PolicyValidationResult failure(List<PolicyValidationError> errors, List<PolicyValidationWarning> warnings) {
        return new PolicyValidationResult(false, errors, warnings != null ? warnings : List.of());
    }

    public static PolicyValidationResult failure(List<PolicyValidationError> errors) {
        return failure(errors, List.of());
    }

    public List<PolicyValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<PolicyValidationWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /** True if document can be bound to runtime source (no errors). */
    public boolean isBindable() {
        return valid;
    }
}
