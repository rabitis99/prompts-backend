package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;
import java.util.Map;

/**
 * 프롬프트 생성 결과와 실행 메타데이터를 함께 반환하는 응답 DTO
 */
public record UnifiedGeneratePromptResponse(

        String output,

        @JsonProperty("requested_engine_mode")
        EngineMode requestedEngineMode,

        @JsonProperty("effective_engine_mode")
        EngineMode effectiveEngineMode,

        @JsonProperty("engine_profile")
        EngineProfile engineProfile,

        @JsonProperty("resolved_category")
        PromptCategory resolvedCategory,

        @JsonProperty("resolved_domain")
        TaskDomain resolvedDomain,

        PromptObjective objective,

        @JsonProperty("output_needs")
        OutputNeeds outputNeeds,

        @JsonProperty("resolved_intent")
        ActionIntent resolvedIntent,

        String variant,

        @JsonProperty("resolved_role")
        RoleTypeInterface resolvedRole,

        @JsonProperty("resolved_action")
        ActionTypeInterface resolvedAction,

        @JsonProperty("quality_badges")
        List<BadgeDto> qualityBadges,

        @JsonProperty("verify_passed")
        boolean verifyPassed,

        @JsonProperty("repair_count")
        int repairCount,

        @JsonProperty("finally_passed")
        boolean finallyPassed,

        @JsonProperty("schema_contract_failed")
        boolean schemaContractFailed,

        @JsonProperty("schema_failure_reasons")
        List<String> schemaFailureReasons,

        @JsonProperty("semantic_profiles_applied")
        List<String> semanticProfilesApplied,

        @JsonProperty("validation_warnings")
        List<String> validationWarnings,

        @JsonProperty("recommendation_hints")
        List<String> recommendationHints,

        @JsonProperty("semantic_resolution_summary")
        String semanticResolutionSummary,

        @JsonProperty("axis_sources")
        Map<String, String> axisSources
) {
}