package org.example.sharedprompts.domain.prompt.application.engine.generation;

import org.example.sharedprompts.domain.prompt.application.engine.contract.SchemaContractEvaluator;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.generate.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.policy.AxisSourcePolicy;
import org.example.sharedprompts.domain.prompt.application.semantic.resolution.ResolutionResult;
import org.example.sharedprompts.domain.prompt.application.semantic.resolution.SemanticResolutionService;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.metrics.PromptEngineMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrchestratorThinTest {

    @Test
    void orchestrator_uses_semantic_resolution_and_returns_semantic_metadata() {
        GeneratePromptUseCase generatePromptUseCase = mock(GeneratePromptUseCase.class);
        when(generatePromptUseCase.generate(any(GeneratePromptCommand.class), any(ConfirmedSemanticAxes.class)))
                .thenAnswer(inv -> {
                    GeneratePromptCommand cmd = inv.getArgument(0);
                    return new GeneratePromptResult(
                            1L,
                            cmd.title(),
                            "output",
                            List.of(),
                            PromptObjective.CREATIVE_WITH_CONSTRAINTS,
                            true,
                            true,
                            0,
                            true
                    );
                });

        // Resolved axes differ from request so we assert result comes from resolution, not from request
        ConfirmedSemanticAxes axes = ConfirmedSemanticAxes.builder()
                .category(PromptCategory.ETC)
                .taskDomain(TaskDomain.GENERAL)
                .intent(ActionIntent.GENERATE)
                .objective(PromptObjective.CREATIVE_WITH_CONSTRAINTS)
                .outputNeeds(OutputNeeds.FREE_FORM)
                .appliedProfileIds(List.of("profile:ETC", "intent:GENERATE"))
                .validationWarnings(List.of())
                .recommendationHints(List.of())
                .build();

        SemanticResolutionService semanticResolutionService = mock(SemanticResolutionService.class);
        ResolutionResult.ResolutionMetadata metadata = ResolutionResult.ResolutionMetadata.userProvidedIntentRoleAction();
        when(semanticResolutionService.resolve(any(UnifiedGeneratePromptCommand.class)))
                .thenReturn(ResolutionResult.Result.ok(axes, metadata));

        SchemaContractEvaluator schemaContractEvaluator = new SchemaContractEvaluator();
        PromptEngineMetrics metrics = new PromptEngineMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        AxisSourcePolicy axisSourcePolicy = new AxisSourcePolicy();
        UnifiedGeneratePromptResultBuilder resultBuilder = new UnifiedGeneratePromptResultBuilder();

        UnifiedPromptGenerationOrchestrator orchestrator = new UnifiedPromptGenerationOrchestrator(
                generatePromptUseCase,
                semanticResolutionService,
                schemaContractEvaluator,
                metrics,
                axisSourcePolicy,
                resultBuilder
        );

        // Request uses different category/intent so result must reflect resolved axes (ETC, GENERATE), not request
        String expectedTitle = "Test Title";
        String expectedDescription = "Test description";
        UnifiedGeneratePromptCommand command = UnifiedGeneratePromptCommand.of(
                1L,
                RequestMode.SIMPLE,
                PromptCategory.ANALYSIS,
                ActionIntent.ANALYZE,
                null,
                "input",
                null,
                EngineMode.AUTO,
                ToneType.NEUTRAL,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                expectedTitle,
                expectedDescription
        );

        UnifiedGeneratePromptResult result = orchestrator.generate(command);

        ArgumentCaptor<UnifiedGeneratePromptCommand> resolveCommandCaptor = ArgumentCaptor.forClass(UnifiedGeneratePromptCommand.class);
        verify(semanticResolutionService).resolve(resolveCommandCaptor.capture());
        UnifiedGeneratePromptCommand passedToResolve = resolveCommandCaptor.getValue();
        assertThat(passedToResolve).isSameAs(command);
        assertThat(passedToResolve.requestMode()).isEqualTo(RequestMode.SIMPLE);

        ArgumentCaptor<GeneratePromptCommand> commandCaptor = ArgumentCaptor.forClass(GeneratePromptCommand.class);
        ArgumentCaptor<ConfirmedSemanticAxes> axesCaptor = ArgumentCaptor.forClass(ConfirmedSemanticAxes.class);
        verify(generatePromptUseCase).generate(commandCaptor.capture(), axesCaptor.capture());
        GeneratePromptCommand passedCommand = commandCaptor.getValue();
        assertThat(axesCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(axes);
        assertThat(passedCommand.promptCategory()).isEqualTo(axes.category());
        assertThat(passedCommand.title()).isEqualTo(expectedTitle);
        assertThat(passedCommand.description()).isEqualTo(expectedDescription);

        // Result must reflect resolved axes (ETC, GENERATE), not request (ANALYSIS, ANALYZE)
        assertThat(result.effectiveEngineMode()).isEqualTo(EngineMode.V2);
        assertThat(result.engineProfile()).isEqualTo(EngineProfile.QUALITY_PIPELINE);
        assertThat(result.resolvedIntent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(result.resolvedCategory()).isEqualTo(PromptCategory.ETC);
        assertThat(result.semanticProfilesApplied()).contains("profile:ETC", "intent:GENERATE");
        assertThat(result.semanticResolutionSummary()).contains("category=ETC");
        assertThat(result.semanticResolutionSummary()).contains("intent=GENERATE");
        assertThat(result.axisSources()).isNotNull();
        assertThat(result.axisSources()).containsEntry("intent", "USER_PROVIDED");
    }
}
