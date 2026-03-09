package org.example.sharedprompts.domain.prompt.application.service.orchestration.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingRuleEngineTest {

    private UnifiedGeneratePromptCommand baseCommand(ActionIntent intent, String jsonSchema) {
        return UnifiedGeneratePromptCommand.of(
                1L,
                RequestMode.SIMPLE,
                PromptCategory.ETC,
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
                null,
                null
        );
    }

    private IntentDefaults baseDefaults(ActionIntent intent, TaskDomain affinity) {
        var resolutionDefaults = IntentDictionary.getResolutionDefaults(intent);
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

        RoutingRuleEngine.RoutingRule lowPriority = new RoutingRuleEngine.RoutingRule(
                "low",
                1,
                new RoutingRuleEngine.RoutingCondition("SUMMARIZE", null, null, null),
                PromptObjective.FACTUAL,
                null,
                null,
                null,
                null,
                null,
                null
        );
        RoutingRuleEngine.RoutingRule highPriority = new RoutingRuleEngine.RoutingRule(
                "high",
                10,
                new RoutingRuleEngine.RoutingCondition("SUMMARIZE", null, null, null),
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

        RoutingRuleEngine.RoutingOverrides overrides = engine.apply(command, defaults);

        assertThat(overrides.objectiveOverride()).isEqualTo(PromptObjective.REASONING);
        assertThat(overrides.appliedRuleIds()).containsExactly("low", "high");
    }

    @Test
    void more_specific_rule_should_win_when_priority_equal() {
        RoutingRuleEngine engine = new RoutingRuleEngine();

        RoutingRuleEngine.RoutingRule generic = new RoutingRuleEngine.RoutingRule(
                "generic",
                5,
                new RoutingRuleEngine.RoutingCondition("EXTRACT", null, null, null),
                null,
                OutputNeeds.JSON_REQUIRED,
                null,
                null,
                null,
                null,
                null
        );
        RoutingRuleEngine.RoutingRule specific = new RoutingRuleEngine.RoutingRule(
                "specific",
                5,
                new RoutingRuleEngine.RoutingCondition("EXTRACT", PromptCategory.ANALYSIS.key(), true, TaskDomain.ANALYTICAL),
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

        UnifiedGeneratePromptCommand command = baseCommand(ActionIntent.EXTRACT, "{}");
        IntentDefaults defaults = baseDefaults(ActionIntent.EXTRACT, TaskDomain.ANALYTICAL);

        RoutingRuleEngine.RoutingOverrides overrides = engine.apply(command, defaults);

        assertThat(overrides.outputNeedsOverride()).isEqualTo(OutputNeeds.JSON_SCHEMA_REQUIRED);
        assertThat(overrides.appliedRuleIds()).containsExactly("generic", "specific");
    }

    @Test
    void tie_should_be_resolved_by_registration_order() {
        RoutingRuleEngine engine = new RoutingRuleEngine();

        RoutingRuleEngine.RoutingRule first = new RoutingRuleEngine.RoutingRule(
                "first",
                5,
                new RoutingRuleEngine.RoutingCondition("PLAN", null, null, null),
                null,
                null,
                null,
                TaskDomain.PRACTICAL,
                null,
                null,
                null
        );
        RoutingRuleEngine.RoutingRule second = new RoutingRuleEngine.RoutingRule(
                "second",
                5,
                new RoutingRuleEngine.RoutingCondition("PLAN", null, null, null),
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

        RoutingRuleEngine.RoutingOverrides overrides = engine.apply(command, defaults);

        assertThat(overrides.domainOverride()).isEqualTo(TaskDomain.PRACTICAL);
        assertThat(overrides.appliedRuleIds()).containsExactly("first", "second");
    }
}

