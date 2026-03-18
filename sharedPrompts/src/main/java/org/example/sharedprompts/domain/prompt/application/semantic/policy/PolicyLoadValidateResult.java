package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;

import java.util.Optional;

/**
 * Result of load → parse → validate (no bind).
 * Used when building versioned document archive for diff/reproducibility.
 */
public record PolicyLoadValidateResult(
        PolicyValidationResult validationResult,
        Optional<ValidatedPolicyBundle> validatedBundle
) {
    public boolean isSuccess() {
        return validationResult != null && validationResult.isBindable() && validatedBundle != null && validatedBundle.isPresent();
    }

    public static PolicyLoadValidateResult failure(PolicyValidationResult validationResult) {
        return new PolicyLoadValidateResult(validationResult, Optional.empty());
    }

    public static PolicyLoadValidateResult success(PolicyValidationResult validationResult, ValidatedPolicyBundle validatedBundle) {
        return new PolicyLoadValidateResult(validationResult, Optional.of(validatedBundle));
    }
}
