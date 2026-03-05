package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.DomainRoleType;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityBadge;

import java.util.List;

/**
 * 통합 프롬프트 생성 결과.
 *
 * <ul>
 *   <li>{@code verifyPassed}: 첫 검증 통과 여부 (수리 전 1차 검증)</li>
 *   <li>{@code finallyPassed}: 수리 후 최종 검증 통과 여부</li>
 * </ul>
 */
public record UnifiedGeneratePromptResult(
        String output,
        EngineMode requestedEngineMode,
        EngineMode effectiveEngineMode,
        PromptCategory category,
        TaskDomain resolvedDomain,
        PromptObjective objective,
        OutputNeeds outputNeeds,
        ActionIntent intent,
        String variant,
        CoreRoleType coreRole,
        DomainRoleType domainRole,
        List<QualityBadge> qualityBadges,
        /** 첫 검증(수리 전) 통과 여부 */
        boolean verifyPassed,
        int repairCount,
        /** 수리 후 최종 검증 통과 여부 */
        boolean finallyPassed,
        boolean schemaContractFailed,
        List<String> schemaFailureReasons,
        EngineProfile engineProfile,
        List<String> appliedRuleIds,
        List<String> routingReasons
) {
    public UnifiedGeneratePromptResult {
        qualityBadges = qualityBadges != null ? List.copyOf(qualityBadges) : List.of();
        schemaFailureReasons = schemaFailureReasons != null ? List.copyOf(schemaFailureReasons) : List.of();
        appliedRuleIds = appliedRuleIds != null ? List.copyOf(appliedRuleIds) : List.of();
        routingReasons = routingReasons != null ? List.copyOf(routingReasons) : List.of();
    }
}

