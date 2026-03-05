package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.service.orchestration.unified.UnifiedRoutingFacade;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.metrics.PromptEngineMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrchestratorThinTest {

    @Test
    void orchestrator_should_delegate_policy_to_routing_facade() {
        // given
        GeneratePromptUseCase generatePromptUseCase = command -> new GeneratePromptResult(
                1L,
                command.title(),
                "output",
                List.of(),
                org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective.CREATIVE,
                true,  // formatValid
                true,
                0,
                true
        );

        UnifiedRoutingFacade.RoutingDecision routingDecision = new UnifiedRoutingFacade.RoutingDecision(
                ActionIntent.GENERATE,
                PromptObjective.CREATIVE,
                OutputNeeds.FREE_FORM,
                TaskDomain.PRACTICAL,
                null,
                null,
                EngineProfile.QUALITY_PIPELINE,
                EngineMode.AUTO,
                EngineMode.V2,
                List.of("r1"),
                List.of("intentDefaults:GENERATE")
        );

        UnifiedRoutingFacade routingFacade = new UnifiedRoutingFacade(
                null,
                null,
                null,
                null
        ) {
            @Override
            public RoutingDecision decide(UnifiedGeneratePromptCommand command) {
                return routingDecision;
            }
        };

        SchemaContractEvaluator schemaContractEvaluator = new SchemaContractEvaluator();
        PromptEngineMetrics metrics = new PromptEngineMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        UnifiedPromptGenerationOrchestrator orchestrator = new UnifiedPromptGenerationOrchestrator(
                generatePromptUseCase,
                routingFacade,
                schemaContractEvaluator,
                metrics
        );

        UnifiedGeneratePromptCommand command = UnifiedGeneratePromptCommand.of(
                1L,
                PromptCategory.ETC,
                ActionIntent.GENERATE,
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
                null
        );

        // when
        UnifiedGeneratePromptResult result = orchestrator.generate(command);

        // then
        assertThat(result.effectiveEngineMode()).isEqualTo(EngineMode.V2);
        assertThat(result.engineProfile()).isEqualTo(EngineProfile.QUALITY_PIPELINE);
        assertThat(result.appliedRuleIds()).containsExactly("r1");
        assertThat(result.routingReasons()).contains("intentDefaults:GENERATE");
    }
}

