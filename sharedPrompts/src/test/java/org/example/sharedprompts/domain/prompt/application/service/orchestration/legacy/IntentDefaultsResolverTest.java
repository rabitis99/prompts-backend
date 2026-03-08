package org.example.sharedprompts.domain.prompt.application.service.orchestration.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentDefaultsResolverTest {

    private final IntentDefaultsResolver resolver = new IntentDefaultsResolver();

    @Test
    void generate_intent_should_use_generate_metadata() {
        UnifiedGeneratePromptCommand command = UnifiedGeneratePromptCommand.of(
                1L,
                RequestMode.SIMPLE,
                PromptCategory.ETC,
                ActionIntent.GENERATE,
                null,
                "hello",
                null,
                EngineMode.AUTO,
                ToneType.NEUTRAL,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        IntentDefaults defaults = resolver.resolve(command);
        var expected = IntentDictionary.getResolutionDefaults(ActionIntent.GENERATE);

        assertThat(defaults.intent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(defaults.objective()).isEqualTo(expected.defaultObjective());
        assertThat(defaults.outputNeeds()).isEqualTo(expected.preferredOutputNeeds());
        assertThat(defaults.responseShape()).isEqualTo(expected.defaultResponseShape());
        assertThat(defaults.domainAffinity()).isNull();
    }

    @Test
    void extract_intent_should_have_extraction_defaults() {
        UnifiedGeneratePromptCommand command = UnifiedGeneratePromptCommand.of(
                1L,
                RequestMode.SIMPLE,
                PromptCategory.ANALYSIS,
                ActionIntent.EXTRACT,
                null,
                "input",
                null,
                EngineMode.AUTO,
                ToneType.NEUTRAL,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        IntentDefaults defaults = resolver.resolve(command);
        var expected = IntentDictionary.getResolutionDefaults(ActionIntent.EXTRACT);

        assertThat(defaults.intent()).isEqualTo(ActionIntent.EXTRACT);
        assertThat(defaults.objective()).isEqualTo(expected.defaultObjective());
        assertThat(defaults.outputNeeds()).isEqualTo(expected.preferredOutputNeeds());
        assertThat(defaults.responseShape()).isEqualTo(expected.defaultResponseShape());
        assertThat(defaults.domainAffinity()).isNull();
    }
}

