package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.DomainRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeSerializer;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;

import java.util.List;

/**
 * 통합 프롬프트 생성 요청 DTO.
 *
 * <p>UX 친화적인 최소 필드만 필수로 노출하고,
 * 세부 튜닝 옵션은 모두 선택/고급 필드로 둔다.</p>
 */
public record UnifiedGeneratePromptRequest(

        /**
         * 상위 카테고리 (선택).
         * null 인 경우 GENERAL 로 폴백한다.
         */
        PromptCategory category,

        /**
         * 상위 작업 의도 (선택).
         * null 인 경우 GENERATE 로 폴백한다.
         */
        ActionIntent intent,

        /**
         * Intent 내부에서 세부 variant 식별자 (선택).
         */
        String variant,

        /**
         * 최소 필수 입력.
         */
        @NotBlank(message = "입력을 입력해주세요.")
        @Size(max = 10_000, message = "입력은 최대 10000자까지 입력해주세요.")
        String input,

        /**
         * 선택 JSON Schema. 존재하면 EXTRACTION + JSON_SCHEMA_REQUIRED 로 고정된다.
         */
        @Size(max = 20_000, message = "JSON Schema는 최대 20000자까지 허용됩니다.")
        @JsonProperty("json_schema")
        String jsonSchema,

        /**
         * 엔진 모드 (AUTO | V2 | V3).
         * null 이면 AUTO 로 동작한다.
         */
        @JsonProperty("engine_mode")
        EngineMode engineMode,

        /**
         * 고급 스타일/톤 옵션 (선택).
         */
        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experience,

        /**
         * 품질 파이프라인 비활성화 플래그 (준비 중).
         * true 요청 시 현재는 400 에러 반환. Verify/Repair 생략 기능은 추후 지원 예정.
         */
        @JsonProperty("disable_quality_pipeline")
        Boolean disableQualityPipeline,

        /**
         * 고급 V2 액션/롤 타입 오버라이드 (선택).
         * 지정되지 않으면 기본 Etc 타입으로 동작한다.
         */
        @JsonProperty("action_type")
        @JsonSerialize(using = ActionTypeSerializer.class)
        @JsonDeserialize(using = ActionTypeDeserializer.class)
        ActionTypeInterface actionType,

        @JsonProperty("role_type")
        @JsonSerialize(using = RoleTypeSerializer.class)
        @JsonDeserialize(using = RoleTypeDeserializer.class)
        RoleTypeInterface roleType,

        /**
         * V3 메타 역할 축 (선택).
         */
        CoreRoleType coreRole,
        DomainRoleType domainRole,

        /**
         * 태그/메타데이터 (선택).
         * 현재는 저장소 연동을 위해서만 사용되며, 없으면 빈 리스트로 처리된다.
         */
        @Size(max = 20, message = "태그는 최대 20개까지 가능합니다.")
        List<@Size(min = 1, max = 50, message = "태그는 1~50자로 입력해주세요.") String> tags
) {

    public UnifiedGeneratePromptCommand toCommand(Long userId) {
        boolean normalizedDisableQualityPipeline = Boolean.TRUE.equals(disableQualityPipeline);
        return UnifiedGeneratePromptCommand.of(
                userId,
                category,
                intent,
                variant,
                input,
                jsonSchema,
                engineMode,
                tone,
                style,
                language,
                experience,
                normalizedDisableQualityPipeline,
                actionType,
                roleType,
                coreRole,
                domainRole,
                tags
        );
    }
}

