package org.example.sharedprompts.domain.prompt.application.service.orchestration.unified;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentDefaultsResolverTest {

    private final IntentDefaultsResolver resolver = new IntentDefaultsResolver();

    @Test
    void generate_intent_should_use_generate_metadata() {
        UnifiedGeneratePromptCommand command = UnifiedGeneratePromptCommand.of(
                1L,
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
                null,
                null
        );

        IntentDefaults defaults = resolver.resolve(command);

        assertThat(defaults.intent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(defaults.objective()).isEqualTo(ActionIntent.GENERATE.getDefaultObjective());
        assertThat(defaults.outputNeeds()).isEqualTo(ActionIntent.GENERATE.getPreferredOutputNeeds());
        assertThat(defaults.responseShape()).isEqualTo(ActionIntent.GENERATE.getDefaultResponseShape());
        assertThat(defaults.domainAffinity()).isNull();
    }

    @Test
    void extract_intent_should_have_extraction_defaults_and_domain_affinity() {
        UnifiedGeneratePromptCommand command = UnifiedGeneratePromptCommand.of(
                1L,
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

        assertThat(defaults.intent()).isEqualTo(ActionIntent.EXTRACT);
        assertThat(defaults.objective()).isEqualTo(ActionIntent.EXTRACT.getDefaultObjective());
        assertThat(defaults.outputNeeds()).isEqualTo(ActionIntent.EXTRACT.getPreferredOutputNeeds());
        assertThat(defaults.responseShape()).isEqualTo(ActionIntent.EXTRACT.getDefaultResponseShape());
        assertThat(defaults.domainAffinity()).isNotNull();
    }
}

