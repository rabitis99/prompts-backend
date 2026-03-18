package org.example.sharedprompts.domain.prompt.application.port.in.generate;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;
import java.util.Map;

/** 통합 프롬프트 생성 결과. axis_sources 등 시맨틱 해석 메타데이터 포함 */
public record UnifiedGeneratePromptResult(
        String output,
        EngineMode requestedEngineMode,
        EngineMode effectiveEngineMode,
        PromptCategory resolvedCategory,
        TaskDomain resolvedDomain,
        PromptObjective objective,
        OutputNeeds outputNeeds,
        ActionIntent resolvedIntent,
        String variant,
        RoleTypeInterface resolvedRole,
        ActionTypeInterface resolvedAction,
        List<QualityBadgeItem> qualityBadges,
        boolean verifyPassed,
        int repairCount,
        boolean finallyPassed,
        boolean schemaContractFailed,
        List<String> schemaFailureReasons,
        EngineProfile engineProfile,
        List<String> semanticProfilesApplied,
        List<String> validationWarnings,
        List<String> recommendationHints,
        String semanticResolutionSummary,
        Map<String, String> axisSources
) {
    public UnifiedGeneratePromptResult {
        qualityBadges = qualityBadges != null ? List.copyOf(qualityBadges) : List.of();
        schemaFailureReasons = schemaFailureReasons != null ? List.copyOf(schemaFailureReasons) : List.of();
        semanticProfilesApplied = semanticProfilesApplied != null ? List.copyOf(semanticProfilesApplied) : List.of();
        validationWarnings = validationWarnings != null ? List.copyOf(validationWarnings) : List.of();
        recommendationHints = recommendationHints != null ? List.copyOf(recommendationHints) : List.of();
        axisSources = axisSources != null ? Map.copyOf(axisSources) : Map.of();
    }
}
