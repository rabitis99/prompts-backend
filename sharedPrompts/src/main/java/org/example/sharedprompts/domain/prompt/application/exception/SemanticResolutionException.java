package org.example.sharedprompts.domain.prompt.application.exception;

import java.util.List;

/**
 * Thrown when semantic resolution fails (e.g. missing category/intent or invalid combination).
 * Controller/exception handler should map to 400 with {@link #getMessages()}.
 */
public class SemanticResolutionException extends RuntimeException {

    private final List<String> messages;

    public SemanticResolutionException(List<String> messages) {
        super(messages != null && !messages.isEmpty() ? String.join("; ", messages) : "Semantic resolution failed");
        this.messages = messages != null ? List.copyOf(messages) : List.of();
    }

    public List<String> getMessages() {
        return messages;
    }
}
