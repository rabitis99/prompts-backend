package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;

/**
 * A suggested fallback when user input is missing or invalid.
 */
public record FallbackCandidate(
        ActionIntent intent,
        String rationale
) {}
