package org.example.sharedprompts.domain.prompt.domain.semantic;

import java.util.List;

/**
 * Result of category-aware semantic validation.
 * Supports valid, invalid, warning, and recommended correction/fallback.
 */
public record SemanticValidationResult(
        boolean valid,
        Severity severity,
        List<SemanticValidationItem> items
) {
    public SemanticValidationResult {
        items = items != null ? List.copyOf(items) : List.of();
    }

    public static SemanticValidationResult valid() {
        return new SemanticValidationResult(true, Severity.NONE, List.of());
    }

    public static SemanticValidationResult invalid(List<SemanticValidationItem> items) {
        return new SemanticValidationResult(false, Severity.ERROR, items);
    }

    public static SemanticValidationResult warning(List<SemanticValidationItem> items) {
        return new SemanticValidationResult(true, Severity.WARNING, items);
    }

    public enum Severity {
        NONE,
        WARNING,
        ERROR
    }

    public record SemanticValidationItem(
            String code,
            String message,
            String field,
            String recommendedValue
    ) {}
}
