package org.example.sharedprompts.domain.prompt.domain.semantic.seed;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileSeedDefinition;

import java.util.List;
import java.util.Set;

/**
 * Default catalog of category profile seeds. Adding a profile-backed category extends this list
 * and adds a dedicated seed class under this package — not {@code buildXxx()} on a central source.
 */
public final class CategorySemanticProfileSeedDefinitions {

    private CategorySemanticProfileSeedDefinitions() {
    }

    /**
     * Categories for which the default registry must assemble a profile. Defined by {@link PromptCategory#canonicalSemanticProfileCategories()}
     * so a new enum constant without a seed entry fails in {@link org.example.sharedprompts.domain.prompt.domain.semantic.impl.DefaultCategorySemanticProfileSeedSource}.
     */
    public static Set<PromptCategory> canonicalProfileCategories() {
        return PromptCategory.canonicalSemanticProfileCategories();
    }

    public static List<CategorySemanticProfileSeedDefinition> defaultDefinitions() {
        return List.of(
                DesignSemanticProfileSeed.DEFINITION,
                DevelopmentSemanticProfileSeed.DEFINITION,
                WritingSemanticProfileSeed.DEFINITION,
                ResearchSemanticProfileSeed.DEFINITION,
                BusinessSemanticProfileSeed.DEFINITION,
                ProductivitySemanticProfileSeed.DEFINITION,
                MarketingSemanticProfileSeed.DEFINITION,
                CustomerSupportSemanticProfileSeed.DEFINITION,
                DataAnalysisSemanticProfileSeed.DEFINITION,
                LegalSemanticProfileSeed.DEFINITION,
                CreativeSemanticProfileSeed.DEFINITION,
                EducationSemanticProfileSeed.DEFINITION,
                EtcSemanticProfileSeed.DEFINITION
        );
    }
}
