package org.example.sharedprompts.domain.prompt.application.engine.generation.legacy;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.experience.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutputContractPlannerTest {

    /** Extraction-unrelated category/intent so schema is the only trigger for EXTRACTION in tests. */
    private UnifiedGeneratePromptCommand commandWithSchema(String jsonSchema) {
        Long userId = 1L;
        RequestMode requestMode = RequestMode.SIMPLE;
        PromptCategory category = PromptCategory.ETC;
        ActionIntent intent = ActionIntent.GENERATE;
        String variant = null;
        String input = "input";
        EngineMode engineMode = EngineMode.AUTO;
        ToneType tone = ToneType.NEUTRAL;
        StyleType style = null;
        LanguageType language = null;
        ExperienceLevel experience = null;
        boolean disableQualityPipeline = false;
        ActionTypeInterface actionType = null;
        RoleTypeInterface roleType = null;
        List<String> tags = null;
        String title = null;
        String description = null;

        return UnifiedGeneratePromptCommand.of(
                userId,
                requestMode,
                category,
                intent,
                variant,
                input,
                jsonSchema,
                engineMode,
                tone,
                style,
                language,
                experience,
                disableQualityPipeline,
                actionType,
                roleType,
                tags,
                title,
                description
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
