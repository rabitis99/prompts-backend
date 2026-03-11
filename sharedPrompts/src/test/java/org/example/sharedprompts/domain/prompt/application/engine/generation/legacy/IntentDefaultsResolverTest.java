package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentDefaultsResolverTest {

    private final IntentDefaultsResolver resolver = new IntentDefaultsResolver();

    private UnifiedGeneratePromptCommand command(ActionIntent intent, PromptCategory category, String input) {
        return UnifiedGeneratePromptCommand.of(
                1L,
                RequestMode.SIMPLE,
                category,
                intent,
                null,
                input,
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
    }

    @Test
    void generate_intent_should_use_generate_metadata() {
        UnifiedGeneratePromptCommand command = command(ActionIntent.GENERATE, PromptCategory.ETC, "hello");

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
        UnifiedGeneratePromptCommand command = command(null, PromptCategory.ETC, "hello");

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
        UnifiedGeneratePromptCommand command = command(ActionIntent.EXTRACT, PromptCategory.ANALYSIS, "input");

        IntentDefaults defaults = resolver.resolve(command);
        assertThat(defaults.intent()).isEqualTo(ActionIntent.EXTRACT);
        assertThat(defaults.objective()).isEqualTo(PromptObjective.EXTRACTION);
        assertThat(defaults.outputNeeds()).isEqualTo(OutputNeeds.JSON_REQUIRED);
        assertThat(defaults.responseShape()).isEqualTo(ResponseShape.STRUCTURED);
        assertThat(defaults.domainAffinity()).isNull();
        assertThat(defaults.recommendedEngineProfile()).isEqualTo(EngineProfile.QUALITY_PIPELINE);
    }
}
