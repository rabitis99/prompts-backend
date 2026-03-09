package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.value.quality.QualityBadge;

import java.util.List;
import java.util.Map;

/**
 * Unified prompt generation result with semantic resolution metadata.
 *
 * <ul>
 *   <li>{@code verifyPassed}: first verification pass (before repair)</li>
 *   <li>{@code finallyPassed}: final verification after repair</li>
 *   <li>{@code axisSources}: optional map (intent, role, action, objective, output_needs) → USER_PROVIDED | RECOMMENDED | FALLBACK</li>
 * </ul>
 *
 * <p>Semantic fields replace legacy routing (appliedRuleIds, routingReasons).</p>
 */
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
        List<QualityBadge> qualityBadges,
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
        axisSources = axisSources != null ? Map.copyOf(axisSources) : null;
    }
}
