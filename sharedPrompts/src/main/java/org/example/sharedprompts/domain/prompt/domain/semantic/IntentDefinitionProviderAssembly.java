package org.example.sharedprompts.domain.prompt.domain.semantic;

import java.util.List;

/**
 * Production composition of intent definition / resolution-defaults providers.
 * <p>
 * {@link IntentDefinitionDataSource} only merges and validates; this class is the single
 * explicit registration point for which providers participate in the shipped catalog.
 * Order is stable: creation → modification → analysis → explanation → planning → decision →
 * research → extraction (matches prior {@code IntentDefinitionDataSource} static lists).
 */
public final class IntentDefinitionProviderAssembly {

    private IntentDefinitionProviderAssembly() {}

    public static List<IntentDefinitionEntriesProvider> productionDefinitionsProviders() {
        return List.of(
                new IntentCreationIntentDefinitionsProvider(),
                new IntentModificationIntentDefinitionsProvider(),
                new IntentAnalysisIntentDefinitionsProvider(),
                new IntentExplanationIntentDefinitionsProvider(),
                new IntentPlanningIntentDefinitionsProvider(),
                new IntentDecisionIntentDefinitionsProvider(),
                new IntentResearchIntentDefinitionsProvider(),
                new IntentExtractionIntentDefinitionsProvider());
    }

    public static List<IntentResolutionDefaultsEntriesProvider> productionResolutionDefaultsProviders() {
        return List.of(
                new IntentCreationIntentResolutionDefaultsProvider(),
                new IntentModificationIntentResolutionDefaultsProvider(),
                new IntentAnalysisIntentResolutionDefaultsProvider(),
                new IntentExplanationIntentResolutionDefaultsProvider(),
                new IntentPlanningIntentResolutionDefaultsProvider(),
                new IntentDecisionIntentResolutionDefaultsProvider(),
                new IntentResearchIntentResolutionDefaultsProvider(),
                new IntentExtractionIntentResolutionDefaultsProvider());
    }

    /**
     * Full production {@link IntentDefinitionDataSource} (same merge result as before this refactor).
     */
    public static IntentDefinitionDataSource productionIntentDefinitionDataSource() {
        return new IntentDefinitionDataSource(
                productionDefinitionsProviders(), productionResolutionDefaultsProviders());
    }

    /**
     * Production {@link IntentDictionary}: indexes and validates completeness over
     * {@link #productionIntentDefinitionDataSource()}.
     */
    public static IntentDictionary productionIntentDictionary() {
        return new IntentDictionary(productionIntentDefinitionDataSource());
    }
}
