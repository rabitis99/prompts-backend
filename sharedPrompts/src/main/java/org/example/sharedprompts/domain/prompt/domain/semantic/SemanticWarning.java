package org.example.sharedprompts.domain.prompt.domain.semantic;

/**
 * A semantic validation warning: non-fatal but worth surfacing to the user or logs.
 */
public record SemanticWarning(
        String code,
        String message,
        String field,
        String recommendedValue,
        String rationale
) {
    public SemanticWarning(String code, String message, String field, String recommendedValue) {
        this(code, message, field, recommendedValue, null);
    }
}
