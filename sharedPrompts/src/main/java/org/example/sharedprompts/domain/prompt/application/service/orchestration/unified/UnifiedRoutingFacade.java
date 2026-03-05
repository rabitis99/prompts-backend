package org.example.sharedprompts.domain.prompt.application.service.orchestration.unified;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.DomainRoleType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified 라우팅 파사드.
 *
 * <p>IntentDefaultsResolver → RoutingRuleEngine → DomainFinalizer → OutputContractPlanner
 * 순서로 작은 서비스를 조합해 최종 RoutingDecision 을 만든다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedRoutingFacade {

    private final IntentDefaultsResolver intentDefaultsResolver;
    private final RoutingRuleEngine routingRuleEngine;
    private final DomainFinalizer domainFinalizer;
    private final OutputContractPlanner outputContractPlanner;

    public RoutingDecision decide(UnifiedGeneratePromptCommand command) {
        List<String> reasons = new ArrayList<>();

        EngineMode requestedMode = command.engineMode() != null ? command.engineMode() : EngineMode.AUTO;

        IntentDefaults defaults = intentDefaultsResolver.resolve(command);
        reasons.add("intentDefaults:" + defaults.intent().name());

        RoutingRuleEngine.RoutingOverrides overrides = routingRuleEngine.apply(command, defaults);
        List<String> appliedRuleIds = overrides.appliedRuleIds();
        if (!appliedRuleIds.isEmpty()) {
            reasons.add("rulesApplied:" + String.join(",", appliedRuleIds));
        }

        PromptObjective objective = overrides.objectiveOverride() != null
                ? overrides.objectiveOverride()
                : defaults.objective();
        OutputNeeds outputNeeds = overrides.outputNeedsOverride() != null
                ? overrides.outputNeedsOverride()
                : defaults.outputNeeds();

        CoreRoleType coreRole = overrides.coreRoleOverride() != null
                ? overrides.coreRoleOverride()
                : defaultCoreRoleForIntent(defaults);
        DomainRoleType domainRole = overrides.domainRoleOverride();

        FinalDomainDecision domainDecision = domainFinalizer.finalizeDomain(command, defaults, overrides);
        TaskDomain finalDomain = domainDecision.domain();
        reasons.addAll(domainDecision.reasons());

        ContractDecision contractDecision = outputContractPlanner.plan(command, objective, outputNeeds);
        objective = contractDecision.objective();
        outputNeeds = contractDecision.outputNeeds();
        reasons.addAll(contractDecision.reasons());

        EngineProfile engineProfile = decideEngineProfile(requestedMode, contractDecision.schemaRequired(), overrides, defaults, reasons);
        EngineMode effectiveMode = mapProfileToEngineMode(engineProfile, requestedMode, reasons);

        log.info("[UnifiedRoutingFacade] requestedMode={}, effectiveMode={}, profile={}, objective={}, outputNeeds={}, domain={}, rules={}",
                requestedMode, effectiveMode, engineProfile, objective, outputNeeds, finalDomain, appliedRuleIds);

        return new RoutingDecision(
                defaults.intent(),
                objective,
                outputNeeds,
                finalDomain,
                coreRole,
                domainRole,
                engineProfile,
                requestedMode,
                effectiveMode,
                appliedRuleIds,
                List.copyOf(reasons)
        );
    }

    private EngineProfile decideEngineProfile(
            EngineMode requestedMode,
            boolean schemaRequired,
            RoutingRuleEngine.RoutingOverrides overrides,
            IntentDefaults defaults,
            List<String> reasons
    ) {
        if (overrides.engineProfileOverride() != null) {
            reasons.add("engineProfile:ruleOverride:" + overrides.engineProfileOverride());
            return overrides.engineProfileOverride();
        }

        if (schemaRequired) {
            reasons.add("engineProfile:jsonStrictBySchema");
            return EngineProfile.JSON_STRICT;
        }

        if (requestedMode == EngineMode.V3) {
            reasons.add("engineProfile:requestedV3->fastPipeline");
            return EngineProfile.FAST_PIPELINE;
        }

        if (requestedMode == EngineMode.V2) {
            reasons.add("engineProfile:requestedV2->qualityPipeline");
            return EngineProfile.QUALITY_PIPELINE;
        }

        // AUTO 인 경우 Intent 기반 추천 프로파일 사용
        reasons.add("engineProfile:autoByIntent:" + defaults.recommendedEngineProfile());
        return defaults.recommendedEngineProfile();
    }

    private EngineMode mapProfileToEngineMode(
            EngineProfile profile,
            EngineMode requestedMode,
            List<String> reasons
    ) {
        // 현재 구현에서는 모든 프로파일이 V2 품질 파이프라인을 사용하므로,
        // EngineMode 관점에서는 V2 로 수렴시킨다.
        // 향후 Fast path 가 구현되면 FAST_PIPELINE -> V3 등으로 분기할 수 있다.
        if (profile == EngineProfile.FAST_PIPELINE && requestedMode != EngineMode.V3) {
            reasons.add("engineMode:fallbackFastToV2");
        }
        return EngineMode.V2;
    }

    private CoreRoleType defaultCoreRoleForIntent(IntentDefaults defaults) {
        switch (defaults.intent()) {
            case SUMMARIZE, ANALYZE, EVALUATE, EXTRACT, CLASSIFY:
                return CoreRoleType.ANALYST;
            case CODE, DEBUG:
                return CoreRoleType.TECHNICAL_EXPERT;
            case DESIGN, GENERATE:
                return CoreRoleType.CREATIVE_DIRECTOR;
            case REWRITE:
                return CoreRoleType.EDITOR;
            case EXPLAIN:
                return CoreRoleType.EDUCATOR;
            case PLAN, DECIDE:
                return CoreRoleType.PROMPT_ENGINEER;
            default:
                return CoreRoleType.GENERALIST;
        }
    }

    public record RoutingDecision(
            org.example.sharedprompts.domain.prompt.common.enums.ActionIntent intent,
            PromptObjective objective,
            OutputNeeds outputNeeds,
            TaskDomain finalDomain,
            CoreRoleType coreRole,
            DomainRoleType domainRole,
            EngineProfile engineProfile,
            EngineMode requestedEngineMode,
            EngineMode effectiveEngineMode,
            List<String> appliedRuleIds,
            List<String> decisionReasons
    ) {
        public RoutingDecision {
            appliedRuleIds = appliedRuleIds != null ? List.copyOf(appliedRuleIds) : List.of();
            decisionReasons = decisionReasons != null ? List.copyOf(decisionReasons) : List.of();
        }
    }
}

