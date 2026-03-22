package org.example.sharedprompts.domain.prompt.domain.semantic.impl;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeed;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedDefinition;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedSource;
import org.example.sharedprompts.domain.prompt.domain.semantic.seed.CategorySemanticProfileSeedDefinitions;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Indexes and validates registered {@link CategorySemanticProfileSeedDefinition} entries; does not own seed payloads.
 * Default wiring uses {@link CategorySemanticProfileSeedDefinitions#defaultDefinitions()}.
 */
public class DefaultCategorySemanticProfileSeedSource implements CategorySemanticProfileSeedSource {

    private final Map<PromptCategory, Supplier<CategorySemanticProfileSeed>> seedSuppliers;
    private final Set<PromptCategory> profileCategories;

    public DefaultCategorySemanticProfileSeedSource() {
        this(CategorySemanticProfileSeedDefinitions.defaultDefinitions());
    }

    /**
     * @param definitions ordered catalog; each canonical category at most once; must not include {@link PromptCategory#EXTRACTION}
     */
    public DefaultCategorySemanticProfileSeedSource(List<CategorySemanticProfileSeedDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("At least one seed definition is required");
        }
        Map<PromptCategory, Supplier<CategorySemanticProfileSeed>> map = new LinkedHashMap<>();
        Set<PromptCategory> seenCanonical = new LinkedHashSet<>();
        for (CategorySemanticProfileSeedDefinition def : definitions) {
            PromptCategory c = def.category().canonical();
            if (c == PromptCategory.EXTRACTION) {
                throw new IllegalStateException("EXTRACTION must not have a category semantic profile seed definition");
            }
            if (!seenCanonical.add(c)) {
                throw new IllegalStateException("Duplicate seed definition for canonical category " + c);
            }
            CategorySemanticProfileSeed sample = def.seedSupplier().get();
            if (sample == null) {
                throw new IllegalStateException("Seed supplier returned null for category " + c);
            }
            if (sample.category().canonical() != c) {
                throw new IllegalStateException(
                        "Seed category mismatch for definition " + c + ": seed has " + sample.category());
            }
            map.put(c, def.seedSupplier());
        }
        Set<PromptCategory> required = PromptCategory.canonicalSemanticProfileCategories();
        if (!map.keySet().equals(required)) {
            Set<PromptCategory> missing = new LinkedHashSet<>(required);
            missing.removeAll(map.keySet());
            Set<PromptCategory> unexpected = new LinkedHashSet<>(map.keySet());
            unexpected.removeAll(required);
            throw new IllegalStateException(
                    "CategorySemanticProfileSeedDefinition catalog must match PromptCategory.canonicalSemanticProfileCategories(); "
                            + "missing=" + missing + ", unexpected=" + unexpected);
        }
        this.seedSuppliers = Map.copyOf(map);
        this.profileCategories = Set.copyOf(new LinkedHashSet<>(map.keySet()));
    }

    @Override
    public Set<PromptCategory> profileCategoriesForRegistry() {
        return profileCategories;
    }

    @Override
    public CategorySemanticProfileSeed requireSeed(PromptCategory category) {
        Objects.requireNonNull(category, "category");
        PromptCategory canonical = category.canonical();
        Supplier<CategorySemanticProfileSeed> supplier = seedSuppliers.get(canonical);
        if (supplier == null) {
            throw new IllegalStateException("Missing CategorySemanticProfileSeed for category: " + canonical.name());
        }
        CategorySemanticProfileSeed seed = supplier.get();
        if (seed == null) {
            throw new IllegalStateException(
                    "CategorySemanticProfileSeed supplier returned null for category: " + canonical.name());
        }
        return seed;
    }

    @Override
    public Optional<CategorySemanticProfileSeed> getSeed(PromptCategory category) {
        Objects.requireNonNull(category, "category");
        PromptCategory canonical = category.canonical();
        Supplier<CategorySemanticProfileSeed> supplier = seedSuppliers.get(canonical);
        if (supplier == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(supplier.get());
    }
}
