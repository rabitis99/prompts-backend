package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.GenerateUnifiedPromptUseCase;
import org.example.sharedprompts.domain.prompt.application.port.in.command.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.query.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.query.UnifiedGeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.service.orchestration.unified.UnifiedRoutingFacade;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.metrics.PromptEngineMetrics;
import org.springframework.stereotype.Service;

/**
 * 단일 엔드포인트를 위한 프롬프트 생성 오케스트레이션 레이어.
 *
 * <p>규칙 기반으로 Objective/OutputNeeds/Domain/EngineMode 를 결정하고,
 * V2 품질 파이프라인을 표준 생성기로 사용한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedPromptGenerationOrchestrator implements GenerateUnifiedPromptUseCase {

    private final GeneratePromptUseCase generatePromptUseCase;
    private final UnifiedRoutingFacade routingFacade;
    private final SchemaContractEvaluator schemaContractEvaluator;
    private final PromptEngineMetrics promptEngineMetrics;

    @Override
    public UnifiedGeneratePromptResult generate(UnifiedGeneratePromptCommand command) {
        long startNs = System.nanoTime();
        UnifiedRoutingFacade.RoutingDecision decision = routingFacade.decide(command);

        GeneratePromptCommand v2Command = toV2Command(command, decision);
        GeneratePromptResult v2Result;
        try {
            v2Result = generatePromptUseCase.generate(v2Command);
        } catch (RuntimeException ex) {
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            promptEngineMetrics.recordFailure(decision.effectiveEngineMode(), latencyMs, ex.getClass().getSimpleName());
            throw ex;
        }

        SchemaContractEvaluator.SchemaContractEvaluation schemaEval =
                schemaContractEvaluator.evaluate(v2Result);

        long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
        promptEngineMetrics.recordSuccess(
                decision.effectiveEngineMode(),
                latencyMs,
                v2Result.repairCount(),
                v2Result.finallyPassed(),
                schemaEval.schemaContractFailed()
        );

        EngineMode requestedMode = command.engineMode() != null ? command.engineMode() : EngineMode.AUTO;

        return new UnifiedGeneratePromptResult(
                v2Result.generatedContent(),
                requestedMode,
                decision.effectiveEngineMode(),
                command.category(),
                decision.finalDomain(),
                decision.objective(),
                decision.outputNeeds(),
                decision.intent(),
                command.variant(),
                decision.coreRole(),
                decision.domainRole(),
                v2Result.badges(),
                v2Result.firstPassSuccess(),
                v2Result.repairCount(),
                v2Result.finallyPassed(),
                schemaEval.schemaContractFailed(),
                schemaEval.schemaFailureReasons(),
                decision.engineProfile(),
                decision.appliedRuleIds(),
                decision.decisionReasons()
        );
    }

    private GeneratePromptCommand toV2Command(
            UnifiedGeneratePromptCommand command,
            UnifiedRoutingFacade.RoutingDecision decision
    ) {
        // V2 파이프라인 내부 식별용 제목. 클라이언트 노출용이면 API 응답에서 별도 title 필드로 교체 가능.
        String syntheticTitle = "[Unified] " + decision.intent().name();
        boolean isPublic = false;

        return new GeneratePromptCommand(
                command.userId(),
                syntheticTitle,
                null,           // description
                isPublic,       // isPublic
                command.category(),
                command.tags(),
                command.input(),
                command.actionType(),
                command.roleType(),
                command.tone(),
                command.style(),
                command.language(),
                command.experience(),
                false,          // useStructuredOutput
                command.jsonSchema()
        );
    }
}

