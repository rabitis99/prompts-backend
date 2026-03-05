package org.example.sharedprompts.domain.prompt.application.service.orchestration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.service.orchestration.unified.UnifiedRoutingFacade;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.DomainRoleType;
import org.springframework.stereotype.Component;

/**
 * Unified 엔진의 라우팅/해석 정책.
 *
 * <p>이 클래스는 과거 God Policy 역할을 수행했으나,
 * 현재는 {@link UnifiedRoutingFacade} 에 위임하는 thin wrapper 이다.
 * 외부에서 이 타입에 직접 의존하던 코드를 점진적으로 Facade 로 이전하기 위한 호환 레이어다.</p>
 */
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

