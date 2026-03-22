package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregates intent definitions/defaults from injected providers (merge, duplicate detection).
 * <p>
 * Does not own provider registration; use {@link IntentDefinitionProviderAssembly} for the
 * production catalog or pass custom lists for tests/alternate wiring.
 * {@link IntentDictionary} performs completeness validation and indexing on top of collected entries.
 */
public final class IntentDefinitionDataSource {

    record IntentDefinitionEntry(ActionIntent intent, IntentDefinition definition) {}

    record IntentResolutionDefaultEntry(ActionIntent intent, IntentResolutionDefaults defaults) {}

    private final List<IntentDefinitionEntriesProvider> definitionsProviders;
    private final List<IntentResolutionDefaultsEntriesProvider> resolutionDefaultsProviders;

    public IntentDefinitionDataSource(
            List<IntentDefinitionEntriesProvider> definitionsProviders,
            List<IntentResolutionDefaultsEntriesProvider> resolutionDefaultsProviders) {
        this.definitionsProviders = List.copyOf(Objects.requireNonNull(definitionsProviders));
        this.resolutionDefaultsProviders = List.copyOf(Objects.requireNonNull(resolutionDefaultsProviders));
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
