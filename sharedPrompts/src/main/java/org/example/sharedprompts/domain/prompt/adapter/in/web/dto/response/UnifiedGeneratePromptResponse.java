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

        /**
         * 최종 생성된 출력.
         */
        String output,

        /**
         * 사용자가 요청한 엔진 모드.
         */
        @JsonProperty("requested_engine_mode")
        EngineMode requestedEngineMode,

        /**
         * 규칙 기반 라우팅 후 실제 사용된 엔진 모드.
         */
        @JsonProperty("effective_engine_mode")
        EngineMode effectiveEngineMode,

        /**
         * 내부 의미 기반 엔진 프로파일.
         */
        @JsonProperty("engine_profile")
        EngineProfile engineProfile,

        /**
         * 해석된 상위 도메인.
         */
        @JsonProperty("resolved_domain")
        TaskDomain resolvedDomain,

        /**
         * 상위 목적/Objective.
         */
        PromptObjective objective,

        /**
         * 출력 형식 요구사항.
         */
        OutputNeeds outputNeeds,

        /**
         * 상위 Intent 및 variant.
         */
        ActionIntent intent,
        String variant,

        /**
         * V3 역할 메타.
         */
        CoreRoleType coreRole,
        DomainRoleType domainRole,

        /**
         * 품질 배지.
         */
        @JsonProperty("quality_badges")
        List<BadgeDto> qualityBadges,

        /**
         * 검증/수정 요약.
         */
        @JsonProperty("verify_passed")
        boolean verifyPassed,

        @JsonProperty("repair_count")
        int repairCount,

        @JsonProperty("finally_passed")
        boolean finallyPassed,

        /**
         * 스키마/계약 위반 여부 및 이유.
         */
        @JsonProperty("schema_contract_failed")
        boolean schemaContractFailed,

        @JsonProperty("schema_failure_reasons")
        List<String> schemaFailureReasons,

        /**
         * 실제 적용된 Rule 식별자 목록.
         */
        @JsonProperty("applied_rule_ids")
        List<String> appliedRuleIds,

        /**
         * Routing/AUTO/강제/override 사유.
         */
        @JsonProperty("routing_reasons")
        List<String> routingReasons
) {
}

