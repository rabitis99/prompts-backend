package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.Optional;

/**
 * Source of category semantic profile seed data.
 * Registry assembles profiles from this source instead of owning seed content inline.
 */
public interface CategorySemanticProfileSeedSource {

    /**
     * Seed for the given category. Empty when this source does not define a profile for the category.
     */
    Optional<CategorySemanticProfileSeed> getSeed(PromptCategory category);
}
