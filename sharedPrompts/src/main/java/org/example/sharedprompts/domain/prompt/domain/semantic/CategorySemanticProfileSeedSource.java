package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.seed.CategorySemanticProfileSeedDefinitions;

import java.util.Set;

/**
 * Source of category semantic profile seed data.
 * Registry assembles profiles from this source instead of owning seed content inline.
 */
public interface CategorySemanticProfileSeedSource {

    /**
     * Categories the registry will assemble. Each must return a non-null seed from {@link #requireSeed(PromptCategory)}.
     * Default: {@link CategorySemanticProfileSeedDefinitions#canonicalProfileCategories()}. Override to assemble a subset only.
     */
    default Set<PromptCategory> profileCategoriesForRegistry() {
        return CategorySemanticProfileSeedDefinitions.canonicalProfileCategories();
    }

    /**
     * Required seed for {@code category} (after {@link PromptCategory#canonical()}).
     * Absence is a configuration error, not an optional outcome.
     */
    CategorySemanticProfileSeed requireSeed(PromptCategory category);
}
