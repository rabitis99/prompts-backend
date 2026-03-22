package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregates intent definitions/defaults provided by smaller providers.
 * <p>
 * IntentDictionary keeps responsibility for fail-fast completeness validation and indexing.
 * Inject custom provider lists via the constructor for tests or alternate wiring; production
 * static aggregation uses {@link #definitionsEntries()} / {@link #resolutionDefaultEntries()}.
 */
public final class IntentDefinitionDataSource {

    record IntentDefinitionEntry(ActionIntent intent, IntentDefinition definition) {}

    record IntentResolutionDefaultEntry(ActionIntent intent, IntentResolutionDefaults defaults) {}

    private static final List<IntentDefinitionEntriesProvider> DEFAULT_DEFINITIONS_PROVIDERS = List.of(
            new IntentCreationIntentDefinitionsProvider(),
            new IntentModificationIntentDefinitionsProvider(),
            new IntentAnalysisIntentDefinitionsProvider(),
            new IntentExplanationIntentDefinitionsProvider(),
            new IntentPlanningIntentDefinitionsProvider(),
            new IntentDecisionIntentDefinitionsProvider(),
            new IntentResearchIntentDefinitionsProvider(),
            new IntentExtractionIntentDefinitionsProvider()
    );

    private static final List<IntentResolutionDefaultsEntriesProvider> DEFAULT_RESOLUTION_DEFAULTS_PROVIDERS = List.of(
            new IntentCreationIntentResolutionDefaultsProvider(),
            new IntentModificationIntentResolutionDefaultsProvider(),
            new IntentAnalysisIntentResolutionDefaultsProvider(),
            new IntentExplanationIntentResolutionDefaultsProvider(),
            new IntentPlanningIntentResolutionDefaultsProvider(),
            new IntentDecisionIntentResolutionDefaultsProvider(),
            new IntentResearchIntentResolutionDefaultsProvider(),
            new IntentExtractionIntentResolutionDefaultsProvider()
    );

    private static final IntentDefinitionDataSource DEFAULT = new IntentDefinitionDataSource(
            DEFAULT_DEFINITIONS_PROVIDERS, DEFAULT_RESOLUTION_DEFAULTS_PROVIDERS);

    private final List<IntentDefinitionEntriesProvider> definitionsProviders;
    private final List<IntentResolutionDefaultsEntriesProvider> resolutionDefaultsProviders;

    public IntentDefinitionDataSource(
            List<IntentDefinitionEntriesProvider> definitionsProviders,
            List<IntentResolutionDefaultsEntriesProvider> resolutionDefaultsProviders) {
        this.definitionsProviders = List.copyOf(Objects.requireNonNull(definitionsProviders));
        this.resolutionDefaultsProviders = List.copyOf(Objects.requireNonNull(resolutionDefaultsProviders));
    }

    static List<IntentDefinitionEntry> definitionsEntries() {
        return DEFAULT.collectDefinitionsEntries();
    }

    static List<IntentResolutionDefaultEntry> resolutionDefaultEntries() {
        return DEFAULT.collectResolutionDefaultEntries();
    }

    public List<IntentDefinitionEntry> collectDefinitionsEntries() {
        Map<ActionIntent, IntentDefinition> defs = new HashMap<>();
        for (IntentDefinitionEntriesProvider provider : definitionsProviders) {
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

    public List<IntentResolutionDefaultEntry> collectResolutionDefaultEntries() {
        Map<ActionIntent, IntentResolutionDefaults> res = new HashMap<>();
        for (IntentResolutionDefaultsEntriesProvider provider : resolutionDefaultsProviders) {
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

