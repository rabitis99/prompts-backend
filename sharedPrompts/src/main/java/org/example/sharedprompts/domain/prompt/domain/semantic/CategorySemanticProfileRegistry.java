package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;

/**
 * Registry of category-aware semantic profiles.
 * Resolves profile by {@link PromptCategory}; no fallback to a generic profile that invents meaning.
 */
public interface CategorySemanticProfileRegistry {

    /**
     * Returns the semantic profile for the category, or null if not found (caller should fail validation).
     */
    CategorySemanticProfile getProfile(PromptCategory category);
}
