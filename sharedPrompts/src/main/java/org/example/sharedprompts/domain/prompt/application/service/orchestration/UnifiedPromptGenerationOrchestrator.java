package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.exception.SemanticResolutionException;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.GenerateUnifiedPromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.service.semantic.SemanticResolutionService;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.metrics.PromptEngineMetrics;
import org.example.sharedprompts.domain.prompt.common.AxisSourceConstants;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Category-aware semantic prompt generation orchestration.
 * <p>
 * <b>axis_sources:</b> Populated here after semantic resolution (per design).
 * Resolution returns {@link SemanticResolutionService.ResolutionMetadata}; this orchestrator
 * builds the final axis_sources map so that response metadata is assembled in one place.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedPromptGenerationOrchestrator implements GenerateUnifiedPromptUseCase {

    private final GeneratePromptUseCase generatePromptUseCase;
    private final SemanticResolutionService semanticResolutionService;
    private final SchemaContractEvaluator schemaContractEvaluator;
    private final PromptEngineMetrics promptEngineMetrics;

    @Override
    public UnifiedGeneratePromptResult generate(UnifiedGeneratePromptCommand command) {
        long startNs = System.nanoTime();

        try {
            SemanticResolutionService.Result resolution = semanticResolutionService.resolve(command);
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
            PromptObjective apiObjective = PromptObjective.fromDomainObjective(axes.objective());

            String summary = "category=" + axes.category()
                    + ", intent=" + axes.intent()
                    + (axes.role().isPresent() ? ", role=" + axes.role().get().key() : "")
                    + (axes.actionType().isPresent() ? ", action=" + axes.actionType().get().key() : "");

            return new UnifiedGeneratePromptResult(
                    v2Result.generatedContent(),
                    requestedMode,
                    EngineMode.V2,
                    axes.category(),
                    axes.taskDomain(),
                    apiObjective,
                    axes.outputNeeds(),
                    axes.intent(),
                    command.variant(),
                    axes.role().orElse(null),
                    axes.actionType().orElse(null),
                    v2Result.badges(),
                    v2Result.firstPassSuccess(),
                    v2Result.repairCount(),
                    v2Result.finallyPassed(),
                    schemaEval.schemaContractFailed(),
                    schemaEval.schemaFailureReasons(),
                    EngineProfile.QUALITY_PIPELINE,
                    axes.appliedProfileIds(),
                    axes.validationWarnings(),
                    axes.recommendationHints(),
                    summary,
                    resolution.metadata() != null ? buildAxisSources(resolution.metadata()) : null
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
                false, // generated prompts are private by default
                axes.category(),
                command.tags(),
                command.input(),
                axes.actionType().orElse(null),
                axes.role().orElse(null),
                axes.tone(),
                axes.style(),
                axes.language(),
                axes.experienceLevel(),
                false, // experimental features disabled by default
                command.jsonSchema()
        );
    }

    /** Builds axis_sources map from resolution metadata (document: populate inside orchestrator after semantic resolution). */
    private static Map<String, String> buildAxisSources(SemanticResolutionService.ResolutionMetadata metadata) {
        if (metadata.isExtraction()) {
            return Map.of(
                    "intent", AxisSourceConstants.IMPLIED_BY_MODE,
                    "objective", AxisSourceConstants.IMPLIED_BY_MODE,
                    "output_needs", AxisSourceConstants.IMPLIED_BY_MODE
            );
        }

        return Map.of(
                "intent", metadata.userProvidedIntent() ? AxisSourceConstants.USER_PROVIDED : AxisSourceConstants.FALLBACK,
                "role", metadata.userProvidedRole() ? AxisSourceConstants.USER_PROVIDED : AxisSourceConstants.RECOMMENDED,
                "action", metadata.userProvidedAction() ? AxisSourceConstants.USER_PROVIDED : AxisSourceConstants.RECOMMENDED,
                "objective", AxisSourceConstants.RECOMMENDED,
                "output_needs", AxisSourceConstants.RECOMMENDED
        );
    }
}
