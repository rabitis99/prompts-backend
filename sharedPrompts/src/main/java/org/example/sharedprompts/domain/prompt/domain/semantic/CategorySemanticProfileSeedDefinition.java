package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * One registered category seed: canonical category + supplier used to materialize {@link CategorySemanticProfileSeed}.
 * The default catalog lists these definitions; {@link org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileSeedSource}
 * indexes and validates them (no seed body here).
 */
public record CategorySemanticProfileSeedDefinition(
        PromptCategory category,
        Supplier<CategorySemanticProfileSeed> seedSupplier
) {
    public CategorySemanticProfileSeedDefinition {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(seedSupplier, "seedSupplier");
        if (category != category.canonical()) {
            throw new IllegalArgumentException(
                    "Seed definition category must be canonical, got " + category + " (canonical=" + category.canonical() + ")");
        }
    }
}
