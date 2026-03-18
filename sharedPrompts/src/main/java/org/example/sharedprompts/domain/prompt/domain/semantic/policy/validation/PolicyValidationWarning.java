package org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation;

import java.util.Optional;

/**
 * Single validation warning. Document may still be bindable when only warnings exist.
 */
public record PolicyValidationWarning(
        String code,
        String message,
        String path
) {
    public PolicyValidationWarning {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        path = path != null ? path : "";
    }

    public static PolicyValidationWarning of(String code, String message) {
        return new PolicyValidationWarning(code, message, "");
    }
}
