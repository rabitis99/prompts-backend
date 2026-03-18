package org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation;

import java.util.Optional;

/**
 * Single validation error. Operations/admin UI can use code, path, and offendingValue.
 */
public record PolicyValidationError(
        String code,
        String message,
        String path,
        Object offendingValue
) {
    public PolicyValidationError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        path = path != null ? path : "";
    }

    public Optional<Object> offendingValueOptional() {
        return Optional.ofNullable(offendingValue);
    }
}
