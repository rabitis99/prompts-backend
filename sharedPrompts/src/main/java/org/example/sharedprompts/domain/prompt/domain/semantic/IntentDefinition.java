package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

/**
 * Formal definition of an {@link ActionIntent} for the Intent Dictionary.
 * Used for documentation, validation messages, and recommendation rationale.
 */
public record IntentDefinition(
        ActionIntent intent,
        String canonicalMeaning,
        String whenToUse,
        String whenNotToUse,
        String distinctionFromNearby,
        String expectedOutputTendencies,
        List<PromptCategory> representativeCategories,
        List<String> representativeActionHints,
        List<String> representativeRoleHints,
        String semanticNotes
) {
    public IntentDefinition {
        representativeCategories = representativeCategories != null ? List.copyOf(representativeCategories) : List.of();
        representativeActionHints = representativeActionHints != null ? List.copyOf(representativeActionHints) : List.of();
        representativeRoleHints = representativeRoleHints != null ? List.copyOf(representativeRoleHints) : List.of();
    }
}
