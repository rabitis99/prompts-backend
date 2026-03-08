package org.example.sharedprompts.domain.prompt.application.service.orchestration.legacy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.DomainRoleType;
import org.springframework.stereotype.Component;

/**
 * Unified 엔진의 라우팅/해석 정책 (legacy).
 *
 * <p><b>Not used by the active pipeline.</b> The orchestrator uses
 * {@link org.example.sharedprompts.domain.prompt.application.service.semantic.SemanticResolutionService}
 * only. This class is in the legacy package and excluded from component scanning.</p>
 *
 * @deprecated Replaced by semantic resolution (SemanticResolutionService). May be removed in a future release.
 */
@Deprecated(since = "semantic-pipeline", forRemoval = true)
@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedRoutingPolicy {

    private final UnifiedRoutingFacade unifiedRoutingFacade;

    public RoutingDecision decide(UnifiedGeneratePromptCommand command) {
        UnifiedRoutingFacade.RoutingDecision facadeDecision = unifiedRoutingFacade.decide(command);
        log.info("[UnifiedRoutingPolicy] Delegated to UnifiedRoutingFacade. requestedMode={}, effectiveMode={}, profile={}, objective={}, outputNeeds={}, domain={}",
                facadeDecision.requestedEngineMode(),
                facadeDecision.effectiveEngineMode(),
                facadeDecision.engineProfile(),
                facadeDecision.objective(),
                facadeDecision.outputNeeds(),
                facadeDecision.finalDomain());

        return new RoutingDecision(
                facadeDecision.objective(),
                facadeDecision.outputNeeds(),
                facadeDecision.finalDomain(),
                facadeDecision.coreRole(),
                facadeDecision.domainRole(),
                facadeDecision.effectiveEngineMode()
        );
    }

    public record RoutingDecision(
            PromptObjective objective,
            OutputNeeds outputNeeds,
            TaskDomain resolvedDomain,
            CoreRoleType coreRole,
            DomainRoleType domainRole,
            EngineMode effectiveEngineMode
    ) {
    }
}
