package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.PolicyValidationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;

import java.util.Optional;

/**
 * Result of load → parse → validate → bind pipeline.
 * Bundle is present only when validation passed (no errors).
 */
public record PolicyLoadValidateBindResult(
        PolicyVersion policyVersion,
        PolicyValidationResult validationResult,
        Optional<PolicyBundle> bundle
) {
    public boolean isSuccess() {
        return validationResult != null && validationResult.isBindable() && bundle != null && bundle.isPresent();
    }

    public static PolicyLoadValidateBindResult failure(PolicyVersion version, PolicyValidationResult validationResult) {
        return new PolicyLoadValidateBindResult(version, validationResult, Optional.empty());
    }

    public static PolicyLoadValidateBindResult success(PolicyVersion version, PolicyValidationResult validationResult, PolicyBundle bundle) {
        return new PolicyLoadValidateBindResult(version, validationResult, Optional.of(bundle));
    }
}
