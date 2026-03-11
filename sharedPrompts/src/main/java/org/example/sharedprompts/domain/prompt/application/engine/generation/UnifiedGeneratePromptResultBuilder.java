package org.example.sharedprompts.domain.prompt.application.engine.generation;

import org.example.sharedprompts.domain.prompt.application.engine.contract.SchemaContractEvaluator;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** UnifiedGeneratePromptResult 조립 담당 */
@Component
public class UnifiedGeneratePromptResultBuilder {

    public UnifiedGeneratePromptResult build(
            GeneratePromptResult v2Result,
            ConfirmedSemanticAxes axes,
            EngineMode requestedEngineMode,
            String variant,
            SchemaContractEvaluator.SchemaContractEvaluation schemaEval,
            Map<String, String> axisSources,
            String semanticResolutionSummary
    ) {
        PromptObjective apiObjective = PromptObjective.fromDomainObjective(axes.objective());
        List<String> schemaReasons = schemaEval != null ? schemaEval.schemaFailureReasons() : List.of();
        boolean schemaFailed = schemaEval != null && schemaEval.schemaContractFailed();
        Map<String, String> normalizedAxisSources =
                axisSources != null ? Map.copyOf(axisSources) : Map.of();

        return new UnifiedGeneratePromptResult(
                v2Result.generatedContent(),
                requestedEngineMode != null ? requestedEngineMode : EngineMode.AUTO,
                EngineMode.V2,
                axes.category(),
                axes.taskDomain(),
                apiObjective,
                axes.outputNeeds(),
                axes.intent(),
                variant,
                axes.role().orElse(null),
                axes.actionType().orElse(null),
                v2Result.badges(),
                v2Result.firstPassSuccess(),
                v2Result.repairCount(),
                v2Result.finallyPassed(),
                schemaFailed,
                schemaReasons,
                EngineProfile.QUALITY_PIPELINE,
                axes.appliedProfileIds(),
                axes.validationWarnings(),
                axes.recommendationHints(),
                semanticResolutionSummary != null ? semanticResolutionSummary : "",
                normalizedAxisSources
        );
    }
}
