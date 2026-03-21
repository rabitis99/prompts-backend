package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.seed.CategorySemanticProfileSeedDefinitions;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Source of category semantic profile seed data.
 * Registry assembles profiles from this source instead of owning seed content inline.
 */
public interface CategorySemanticProfileSeedSource {

    /**
     * Categories the registry will assemble. Each must succeed {@link #requireSeed(PromptCategory)}.
     * Default: full {@link PromptCategory#canonicalSemanticProfileCategories()}. Override together with {@link #getSeed}
     * when intentionally wiring a subset (otherwise default iteration will call {@code requireSeed} for missing categories).
     */
    default Set<PromptCategory> profileCategoriesForRegistry() {
        return CategorySemanticProfileSeedDefinitions.canonicalProfileCategories();
    }

    /**
     * Resolves seed for {@code category} (after {@link PromptCategory#canonical()}). Missing registration is a configuration error.
     */
    default CategorySemanticProfileSeed requireSeed(PromptCategory category) {
        Objects.requireNonNull(category, "category");
        PromptCategory canonical = category.canonical();
        return getSeed(canonical).orElseThrow(() -> new IllegalStateException(
                "Missing CategorySemanticProfileSeed for category: " + canonical.name()));
    }

    /**
     * Optional seed when the category may legitimately lack one (e.g. not in this source). Prefer {@link #requireSeed(PromptCategory)} for profile assembly.
     */
    Optional<CategorySemanticProfileSeed> getSeed(PromptCategory category);
}
