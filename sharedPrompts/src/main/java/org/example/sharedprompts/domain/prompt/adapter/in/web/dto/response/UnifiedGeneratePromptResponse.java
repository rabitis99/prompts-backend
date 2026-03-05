package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;
import org.example.sharedprompts.domain.prompt.common.enums.EngineProfile;
import org.example.sharedprompts.domain.prompt.common.enums.OutputNeeds;
import org.example.sharedprompts.domain.prompt.common.enums.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.DomainRoleType;

import java.util.List;

/**
 * 통합 프롬프트 생성 응답 DTO.
 *
 * <p>단일 안정된 계약으로, 출력과 메타데이터를 함께 반환한다.</p>
 */
public record UnifiedGeneratePromptResponse(

        String output,

        @JsonProperty("requested_engine_mode")
        EngineMode requestedEngineMode,

        @JsonProperty("effective_engine_mode")
        EngineMode effectiveEngineMode,

        @JsonProperty("engine_profile")
        EngineProfile engineProfile,

        @JsonProperty("resolved_domain")
        TaskDomain resolvedDomain,

        PromptObjective objective,

        @JsonProperty("output_needs")
        OutputNeeds outputNeeds,

        ActionIntent intent,
        String variant,

        @JsonProperty("core_role")
        CoreRoleType coreRole,

        @JsonProperty("domain_role")
        DomainRoleType domainRole,

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

        @JsonProperty("applied_rule_ids")
        List<String> appliedRuleIds,

        @JsonProperty("routing_reasons")
        List<String> routingReasons
) {
}

