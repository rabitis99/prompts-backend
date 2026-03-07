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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrchestratorThinTest {

    @Test
    void orchestrator_should_delegate_policy_to_routing_facade() {
        // given
        GeneratePromptUseCase generatePromptUseCase = command -> new GeneratePromptResult(
                1L,
                command.title(),
                "output",
                List.of(),
                org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective.CREATIVE_WITH_CONSTRAINTS,
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

        UnifiedRoutingFacade routingFacade = mock(UnifiedRoutingFacade.class);
        when(routingFacade.decide(any(UnifiedGeneratePromptCommand.class))).thenReturn(routingDecision);

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
                null,           // variant
                "input",
                null,           // jsonSchema
                EngineMode.AUTO,
                ToneType.NEUTRAL,
                null,           // style
                null,           // language
                null,           // experience
                null,           // disableQualityPipeline
                null,           // actionType
                null,           // roleType
                null,           // coreRole
                null,           // domainRole
                null,           // tags
                null,           // title
                null            // description
        );

        // when
        UnifiedGeneratePromptResult result = orchestrator.generate(command);

        // then
        verify(routingFacade).decide(any(UnifiedGeneratePromptCommand.class));
        assertThat(result.effectiveEngineMode()).isEqualTo(EngineMode.V2);
        assertThat(result.engineProfile()).isEqualTo(EngineProfile.QUALITY_PIPELINE);
        assertThat(result.appliedRuleIds()).containsExactly("r1");
        assertThat(result.routingReasons()).contains("intentDefaults:GENERATE");
    }
}

