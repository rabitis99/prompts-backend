package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.CoreRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.DomainRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeSerializer;

import java.util.List;

/**
 * 고급 오버라이드 모드 요청.
 */
public record AdvancedGeneratePromptRequest(

        @JsonProperty("request_type")
        RequestType requestType,

        PromptCategory category,
        ActionIntent intent,
        String variant,

        @NotBlank(message = "입력을 입력해주세요.")
        @Size(max = 10_000, message = "입력은 최대 10000자까지 입력해주세요.")
        String input,

        @Size(max = 200, message = "제목은 최대 200자까지 입력해주세요.")
        String title,

        @Size(max = 5_000, message = "설명은 최대 5000자까지 입력해주세요.")
        String description,

        @Size(max = 20_000, message = "JSON Schema는 최대 20000자까지 허용됩니다.")
        @JsonProperty("json_schema")
        String jsonSchema,

        @JsonProperty("engine_mode")
        EngineMode engineMode,

        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experience,

        @JsonProperty("disable_quality_pipeline")
        Boolean disableQualityPipeline,

        @JsonProperty("action_type")
        @JsonSerialize(using = ActionTypeSerializer.class)
        @JsonDeserialize(using = ActionTypeDeserializer.class)
        ActionTypeInterface actionType,

        @JsonProperty("role_type")
        @JsonSerialize(using = RoleTypeSerializer.class)
        @JsonDeserialize(using = RoleTypeDeserializer.class)
        RoleTypeInterface roleType,

        @JsonProperty("core_role")
        CoreRoleType coreRole,

        @JsonProperty("domain_role")
        DomainRoleType domainRole,

        @Size(max = 20, message = "태그는 최대 20개까지 가능합니다.")
        List<@Size(min = 1, max = 50, message = "태그는 1~50자로 입력해주세요.") String> tags
) implements UnifiedGeneratePromptRequest {

    public AdvancedGeneratePromptRequest {
        if (requestType != RequestType.ADVANCED) {
            throw new IllegalArgumentException("request_type must be ADVANCED for AdvancedGeneratePromptRequest");
        }
    }

    @Override
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
                tags,
                title,
                description
        );
    }
}

