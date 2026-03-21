package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates intent definitions/defaults provided by smaller providers.
 * <p>
 * IntentDictionary keeps responsibility for fail-fast completeness validation and indexing.
 */
final class IntentDefinitionDataSource {

    record IntentDefinitionEntry(ActionIntent intent, IntentDefinition definition) {}

    record IntentResolutionDefaultEntry(ActionIntent intent, IntentResolutionDefaults defaults) {}

    private static final List<IntentDefinitionEntriesProvider> DEFINITIONS_PROVIDERS = List.of(
            new IntentCreationIntentDefinitionsProvider(),
            new IntentModificationIntentDefinitionsProvider(),
            new IntentAnalysisIntentDefinitionsProvider(),
            new IntentExplanationIntentDefinitionsProvider(),
            new IntentPlanningIntentDefinitionsProvider(),
            new IntentDecisionIntentDefinitionsProvider(),
            new IntentResearchIntentDefinitionsProvider(),
            new IntentExtractionIntentDefinitionsProvider()
    );

    private static final List<IntentResolutionDefaultsEntriesProvider> RESOLUTION_DEFAULTS_PROVIDERS = List.of(
            new IntentCreationIntentResolutionDefaultsProvider(),
            new IntentModificationIntentResolutionDefaultsProvider(),
            new IntentAnalysisIntentResolutionDefaultsProvider(),
            new IntentExplanationIntentResolutionDefaultsProvider(),
            new IntentPlanningIntentResolutionDefaultsProvider(),
            new IntentDecisionIntentResolutionDefaultsProvider(),
            new IntentResearchIntentResolutionDefaultsProvider(),
            new IntentExtractionIntentResolutionDefaultsProvider()
    );

    private IntentDefinitionDataSource() {}

    static List<IntentDefinitionEntry> definitionsEntries() {
        Map<ActionIntent, IntentDefinition> defs = new HashMap<>();
        for (IntentDefinitionEntriesProvider provider : DEFINITIONS_PROVIDERS) {
            for (IntentDefinitionEntry e : provider.entries()) {
                if (defs.containsKey(e.intent())) {
                    throw new IllegalStateException(
                            "Duplicate IntentDefinition for " + e.intent()
                                    + " from provider " + provider.getClass().getSimpleName());
                }
                defs.put(e.intent(), e.definition());
            }
        }
        return defs.entrySet().stream()
                .map(e -> new IntentDefinitionEntry(e.getKey(), e.getValue()))
                .toList();
    }

    static List<IntentResolutionDefaultEntry> resolutionDefaultEntries() {
        Map<ActionIntent, IntentResolutionDefaults> res = new HashMap<>();
        for (IntentResolutionDefaultsEntriesProvider provider : RESOLUTION_DEFAULTS_PROVIDERS) {
            for (IntentResolutionDefaultEntry e : provider.entries()) {
                if (res.containsKey(e.intent())) {
                    throw new IllegalStateException(
                            "Duplicate IntentResolutionDefaults for " + e.intent()
                                    + " from provider " + provider.getClass().getSimpleName());
                }
                res.put(e.intent(), e.defaults());
            }
        }
        return res.entrySet().stream()
                .map(e -> new IntentResolutionDefaultEntry(e.getKey(), e.getValue()))
                .toList();
    }
}

