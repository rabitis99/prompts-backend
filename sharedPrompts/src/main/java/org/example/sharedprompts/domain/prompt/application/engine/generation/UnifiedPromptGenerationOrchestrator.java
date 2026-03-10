package org.example.sharedprompts.domain.prompt.application.engine.generation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.exception.SemanticResolutionException;
import org.example.sharedprompts.domain.prompt.application.engine.contract.SchemaContractEvaluator;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.GenerateUnifiedPromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.policy.AxisSourcePolicy;
import org.example.sharedprompts.domain.prompt.application.semantic.resolution.ResolutionResult;
import org.example.sharedprompts.domain.prompt.application.semantic.resolution.SemanticResolutionService;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.metrics.PromptEngineMetrics;
import org.springframework.stereotype.Service;

/** 시맨틱 해석 후 프롬프트 생성 오케스트레이션. axis_sources 구성 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedPromptGenerationOrchestrator implements GenerateUnifiedPromptUseCase {

    private final GeneratePromptUseCase generatePromptUseCase;
    private final SemanticResolutionService semanticResolutionService;
    private final SchemaContractEvaluator schemaContractEvaluator;
    private final PromptEngineMetrics promptEngineMetrics;
    private final AxisSourcePolicy axisSourcePolicy;
    private final UnifiedGeneratePromptResultBuilder resultBuilder;

    @Override
    public UnifiedGeneratePromptResult generate(UnifiedGeneratePromptCommand command) {
        long startNs = System.nanoTime();

        try {
            ResolutionResult.Result resolution = semanticResolutionService.resolve(command);
            if (!resolution.success()) {
                throw new SemanticResolutionException(resolution.errors());
            }
            ConfirmedSemanticAxes axes = resolution.axes();

            GeneratePromptCommand v2Command = toV2Command(command, axes);
            GeneratePromptResult v2Result = generatePromptUseCase.generate(v2Command, axes);

            SchemaContractEvaluator.SchemaContractEvaluation schemaEval =
                    schemaContractEvaluator.evaluate(v2Result);

            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            promptEngineMetrics.recordSuccess(
                    EngineMode.V2,
                    latencyMs,
                    v2Result.repairCount(),
                    v2Result.finallyPassed(),
                    schemaEval.schemaContractFailed()
            );

            EngineMode requestedMode = command.engineMode() != null ? command.engineMode() : EngineMode.AUTO;
            String summary = "category=" + axes.category()
                    + ", intent=" + axes.intent()
                    + (axes.role().isPresent() ? ", role=" + axes.role().get().key() : "")
                    + (axes.actionType().isPresent() ? ", action=" + axes.actionType().get().key() : "");

            var metadata = resolution.metadata();
            var axisSources = metadata != null
                    ? axisSourcePolicy.fromResolutionMetadata(
                            metadata.isExtraction(),
                            metadata.userProvidedIntent(),
                            metadata.userProvidedRole(),
                            metadata.userProvidedAction(),
                            metadata.fallbackIntentUsed())
                    : null;

            return resultBuilder.build(
                    v2Result,
                    axes,
                    requestedMode,
                    command.variant(),
                    schemaEval,
                    axisSources,
                    summary
            );
        } catch (RuntimeException ex) {
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            promptEngineMetrics.recordFailure(EngineMode.V2, latencyMs, ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private GeneratePromptCommand toV2Command(UnifiedGeneratePromptCommand command, ConfirmedSemanticAxes axes) {
        String title = (command.title() != null && !command.title().isBlank())
                ? command.title()
                : "[Unified] " + axes.intent().name();
        String description = (command.description() != null && !command.description().isBlank())
                ? command.description()
                : null;

        return new GeneratePromptCommand(
                command.userId(),
                title,
                description,
                false,
                axes.category(),
                command.tags(),
                command.input(),
                axes.actionType().orElse(null),
                axes.role().orElse(null),
                axes.tone(),
                axes.style(),
                axes.language(),
                axes.experienceLevel(),
                false,
                command.jsonSchema()
        );
    }
}
