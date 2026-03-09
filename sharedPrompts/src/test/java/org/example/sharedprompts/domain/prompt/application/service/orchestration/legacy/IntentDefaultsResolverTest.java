package org.example.sharedprompts.domain.prompt.application.service.orchestration.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.ResponseShape;
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
                null
        );

        IntentDefaults defaults = resolver.resolve(command);
        // Use fixed expected values so mapping bugs in IntentDictionary are caught by this test
        assertThat(defaults.intent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(defaults.objective()).isEqualTo(PromptObjective.CREATIVE);
        assertThat(defaults.outputNeeds()).isEqualTo(OutputNeeds.FREE_FORM);
        assertThat(defaults.responseShape()).isEqualTo(ResponseShape.NARRATIVE);
        assertThat(defaults.domainAffinity()).isNull();
        assertThat(defaults.recommendedEngineProfile()).isEqualTo(EngineProfile.QUALITY_PIPELINE);
    }

    @Test
    void null_intent_should_default_to_generate_and_fill_recommended_engine_profile() {
        UnifiedGeneratePromptCommand command = UnifiedGeneratePromptCommand.of(
                1L,
                RequestMode.SIMPLE,
                PromptCategory.ETC,
                null,
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
                null
        );

        IntentDefaults defaults = resolver.resolve(command);
        assertThat(defaults.intent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(defaults.objective()).isEqualTo(PromptObjective.CREATIVE);
        assertThat(defaults.outputNeeds()).isEqualTo(OutputNeeds.FREE_FORM);
        assertThat(defaults.responseShape()).isEqualTo(ResponseShape.NARRATIVE);
        assertThat(defaults.domainAffinity()).isNull();
        assertThat(defaults.recommendedEngineProfile()).isEqualTo(EngineProfile.QUALITY_PIPELINE);
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
                null
        );

        IntentDefaults defaults = resolver.resolve(command);
        assertThat(defaults.intent()).isEqualTo(ActionIntent.EXTRACT);
        assertThat(defaults.objective()).isEqualTo(PromptObjective.EXTRACTION);
        assertThat(defaults.outputNeeds()).isEqualTo(OutputNeeds.JSON_REQUIRED);
        assertThat(defaults.responseShape()).isEqualTo(ResponseShape.STRUCTURED);
        assertThat(defaults.domainAffinity()).isNull();
        assertThat(defaults.recommendedEngineProfile()).isEqualTo(EngineProfile.QUALITY_PIPELINE);
    }
}

