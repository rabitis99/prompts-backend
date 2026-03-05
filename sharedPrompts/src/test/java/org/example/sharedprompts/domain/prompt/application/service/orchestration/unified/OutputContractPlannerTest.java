package org.example.sharedprompts.domain.prompt.application.service.orchestration.unified;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputContractPlannerTest {

    private UnifiedGeneratePromptCommand commandWithSchema(String schema) {
        return UnifiedGeneratePromptCommand.of(
                1L,
                PromptCategory.ANALYSIS,
                ActionIntent.EXTRACT,
                null,
                "input",
                schema,
                EngineMode.AUTO,
                ToneType.NEUTRAL,
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
    void json_schema_should_force_extraction_and_schema_required() {
        OutputContractPlanner planner = new OutputContractPlanner();

        ContractDecision decision = planner.plan(
                commandWithSchema("{\"type\":\"object\"}"),
                PromptObjective.CREATIVE,
                OutputNeeds.FREE_FORM
        );

        assertThat(decision.objective()).isEqualTo(PromptObjective.EXTRACTION);
        assertThat(decision.outputNeeds()).isEqualTo(OutputNeeds.JSON_SCHEMA_REQUIRED);
        assertThat(decision.schemaRequired()).isTrue();
        assertThat(decision.reasons())
                .anySatisfy(r -> assertThat(r).contains("jsonSchema"));
    }
}

