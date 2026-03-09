package org.example.sharedprompts.domain.prompt.application.exception;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when semantic resolution fails (e.g. missing category/intent or invalid combination).
 * Controller/exception handler should map to 400 with {@link #getMessages()}.
 */
public class SemanticResolutionException extends RuntimeException {

    private final List<String> messages;

    public SemanticResolutionException(List<String> messages) {
        super(toDetailMessage(messages));
        this.messages = sanitize(messages);
    }

    public List<String> getMessages() {
        return messages;
    }

    private static String toDetailMessage(List<String> messages) {
        List<String> sanitized = sanitize(messages);
        return sanitized.isEmpty() ? "Semantic resolution failed" : String.join("; ", sanitized);
    }

    private static List<String> sanitize(List<String> messages) {
        return messages == null ? List.of() : messages.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
