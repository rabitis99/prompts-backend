package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntentDefinitionDataSourceTest {

    @Test
    void productionDataSource_coversEveryActionIntent_forDefinitionsAndDefaults() {
        IntentDefinitionDataSource ds =
                IntentDefinitionProviderAssembly.productionIntentDefinitionDataSource();
        var defs = ds.collectDefinitionsEntries();
        var defaults = ds.collectResolutionDefaultEntries();

        assertThat(defs).hasSize(ActionIntent.values().length);
        assertThat(defaults).hasSize(ActionIntent.values().length);

        assertThat(defs.stream().map(IntentDefinitionDataSource.IntentDefinitionEntry::intent).toList())
                .containsExactlyInAnyOrder(ActionIntent.values());
        assertThat(defaults.stream().map(IntentDefinitionDataSource.IntentResolutionDefaultEntry::intent).toList())
                .containsExactlyInAnyOrder(ActionIntent.values());
    }

    @Test
    void reducedDataSource_canBeBuiltWithSubsetOfProviders() {
        IntentDefinitionDataSource ds = new IntentDefinitionDataSource(
                List.of(new IntentCreationIntentDefinitionsProvider()),
                List.of(new IntentCreationIntentResolutionDefaultsProvider()));

        assertThat(ds.collectDefinitionsEntries()).isNotEmpty();
        assertThat(ds.collectResolutionDefaultEntries()).isNotEmpty();
    }

    @Test
    void duplicateIntentAcrossProviders_failsFastOnMerge() {
        IntentDefinitionDataSource ds = new IntentDefinitionDataSource(
                List.of(
                        new IntentCreationIntentDefinitionsProvider(),
                        new IntentCreationIntentDefinitionsProvider()),
                List.of());

        assertThatThrownBy(ds::collectDefinitionsEntries)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate IntentDefinition");
    }

    @Test
    void duplicateResolutionDefaultsAcrossProviders_failsFastOnMerge() {
        IntentDefinitionDataSource ds = new IntentDefinitionDataSource(
                List.of(),
                List.of(
                        new IntentCreationIntentResolutionDefaultsProvider(),
                        new IntentCreationIntentResolutionDefaultsProvider()));

        assertThatThrownBy(ds::collectResolutionDefaultEntries)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate IntentResolutionDefaults");
    }

    @Test
    void incompleteMaps_failIntentDictionaryCompletenessCheck() {
        IntentDefinitionDataSource ds = new IntentDefinitionDataSource(
                List.of(new IntentCreationIntentDefinitionsProvider()),
                List.of(new IntentCreationIntentResolutionDefaultsProvider()));

        Map<ActionIntent, IntentDefinition> defs = new HashMap<>();
        for (IntentDefinitionDataSource.IntentDefinitionEntry e : ds.collectDefinitionsEntries()) {
            defs.put(e.intent(), e.definition());
        }
        Map<ActionIntent, IntentResolutionDefaults> res = new HashMap<>();
        for (IntentDefinitionDataSource.IntentResolutionDefaultEntry e : ds.collectResolutionDefaultEntries()) {
            res.put(e.intent(), e.defaults());
        }

        assertThat(defs.keySet()).isNotEqualTo(EnumSet.allOf(ActionIntent.class));

        assertThatThrownBy(() -> IntentDictionary.validateIntentCoverageCompleteness(defs, res))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IntentDictionary is incomplete");
    }
}
