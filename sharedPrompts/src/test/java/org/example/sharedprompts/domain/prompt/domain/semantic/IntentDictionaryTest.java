package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntentDictionaryTest {

    @Test
    void productionAssembly_buildsFullDictionary_withStableLookups() {
        IntentDictionary dict = IntentDefinitionProviderAssembly.productionIntentDictionary();

        assertThat(dict.get(ActionIntent.GENERATE)).isPresent();
        assertThat(dict.getResolutionDefaults(ActionIntent.GENERATE).defaultObjective()).isNotNull();
    }

    @Test
    void reducedDataSource_canBeWrapped_whenComplete() {
        IntentDefinitionDataSource ds = new IntentDefinitionDataSource(
                IntentDefinitionProviderAssembly.productionDefinitionsProviders(),
                IntentDefinitionProviderAssembly.productionResolutionDefaultsProviders());
        IntentDictionary dict = new IntentDictionary(ds);

        assertThat(dict.getOrThrow(ActionIntent.PLAN)).isNotNull();
    }

    @Test
    void incompleteDataSource_failsFastOnConstruction() {
        IntentDefinitionDataSource incomplete = new IntentDefinitionDataSource(
                List.of(new IntentCreationIntentDefinitionsProvider()),
                List.of(new IntentCreationIntentResolutionDefaultsProvider()));

        assertThatThrownBy(() -> new IntentDictionary(incomplete))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IntentDictionary is incomplete");
    }

    @Test
    void distinctInstances_doNotShareMutableState() {
        IntentDictionary a = IntentDefinitionProviderAssembly.productionIntentDictionary();
        IntentDictionary b = IntentDefinitionProviderAssembly.productionIntentDictionary();

        assertThat(a).isNotSameAs(b);
        assertThat(a.getResolutionDefaults(ActionIntent.GENERATE).defaultObjective())
                .isEqualTo(b.getResolutionDefaults(ActionIntent.GENERATE).defaultObjective());
    }
}
