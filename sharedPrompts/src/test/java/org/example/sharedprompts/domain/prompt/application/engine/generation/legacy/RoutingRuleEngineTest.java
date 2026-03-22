package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.output.ResponseShape;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDefinitionProviderAssembly;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingRuleEngineTest {

    private final IntentDictionary intentDictionary = IntentDefinitionProviderAssembly.productionIntentDictionary();

    private UnifiedGeneratePromptCommand baseCommand(ActionIntent intent, String jsonSchema) {
        return baseCommand(intent, jsonSchema, PromptCategory.ETC);
    }

    private UnifiedGeneratePromptCommand baseCommand(ActionIntent intent, String jsonSchema, PromptCategory category) {
        return UnifiedGeneratePromptCommand.of(
                1L,
                RequestMode.SIMPLE,
                category,
                intent,
                null,
                "input",
                jsonSchema,
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

    private IntentDefaults baseDefaults(ActionIntent intent, TaskDomain affinity) {
        var resolutionDefaults = intentDictionary.getResolutionDefaults(intent);
        return new IntentDefaults(
                intent,
                resolutionDefaults.defaultObjective(),
                resolutionDefaults.preferredOutputNeeds(),
                resolutionDefaults.defaultResponseShape(),
                affinity,
                EngineProfile.QUALITY_PIPELINE
        );
    }

    @Test
    void higher_priority_rule_should_win() {
        RoutingRuleEngine engine = new RoutingRuleEngine();

        RoutingRule lowPriority = new RoutingRule(
                "low",
                1,
                new RoutingCondition("SUMMARIZE", null, null, null),
                PromptObjective.FACTUAL,
                null,
                null,
                null,
                null,
                null,
                null
        );
        RoutingRule highPriority = new RoutingRule(
                "high",
                10,
                new RoutingCondition("SUMMARIZE", null, null, null),
                PromptObjective.REASONING,
                null,
                null,
                null,
                null,
                null,
                null
        );

        engine.registerRule(lowPriority);
        engine.registerRule(highPriority);

        UnifiedGeneratePromptCommand command = baseCommand(ActionIntent.SUMMARIZE, null);
        IntentDefaults defaults = baseDefaults(ActionIntent.SUMMARIZE, TaskDomain.ANALYTICAL);

        RoutingOverrides overrides = engine.apply(command, defaults);

        assertThat(overrides.objectiveOverride()).isEqualTo(PromptObjective.REASONING);
        assertThat(overrides.appliedRuleIds()).containsExactly("high", "low");
    }

    @Test
    void more_specific_rule_should_win_when_priority_equal() {
        RoutingRuleEngine engine = new RoutingRuleEngine();

        RoutingRule generic = new RoutingRule(
                "generic",
                5,
                new RoutingCondition("EXTRACT", null, null, null),
                null,
                OutputNeeds.JSON_REQUIRED,
                null,
                null,
                null,
                null,
                null
        );
        RoutingRule specific = new RoutingRule(
                "specific",
                5,
                new RoutingCondition("EXTRACT", PromptCategory.ANALYSIS.key(), true, TaskDomain.ANALYTICAL),
                null,
                OutputNeeds.JSON_SCHEMA_REQUIRED,
                null,
                null,
                null,
                null,
                null
        );

        engine.registerRule(generic);
        engine.registerRule(specific);

        UnifiedGeneratePromptCommand command =
                baseCommand(ActionIntent.EXTRACT, "{}", PromptCategory.ANALYSIS);
        IntentDefaults defaults = baseDefaults(ActionIntent.EXTRACT, TaskDomain.ANALYTICAL);

        RoutingOverrides overrides = engine.apply(command, defaults);

        assertThat(overrides.outputNeedsOverride()).isEqualTo(OutputNeeds.JSON_SCHEMA_REQUIRED);
        assertThat(overrides.appliedRuleIds()).containsExactly("specific", "generic");
    }

    @Test
    void tie_should_be_resolved_by_registration_order() {
        RoutingRuleEngine engine = new RoutingRuleEngine();

        RoutingRule first = new RoutingRule(
                "first",
                5,
                new RoutingCondition("PLAN", null, null, null),
                null,
                null,
                null,
                TaskDomain.PRACTICAL,
                null,
                null,
                null
        );
        RoutingRule second = new RoutingRule(
                "second",
                5,
                new RoutingCondition("PLAN", null, null, null),
                null,
                null,
                null,
                TaskDomain.ANALYTICAL,
                null,
                null,
                null
        );

        engine.registerRule(first);
        engine.registerRule(second);

        UnifiedGeneratePromptCommand command = baseCommand(ActionIntent.PLAN, null);
        IntentDefaults defaults = baseDefaults(ActionIntent.PLAN, TaskDomain.PRACTICAL);

        RoutingOverrides overrides = engine.apply(command, defaults);

        assertThat(overrides.domainOverride()).isEqualTo(TaskDomain.PRACTICAL);
        assertThat(overrides.appliedRuleIds()).containsExactly("first", "second");
    }
}
