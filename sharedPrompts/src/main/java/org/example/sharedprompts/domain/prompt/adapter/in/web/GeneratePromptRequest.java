package org.example.sharedprompts.domain.prompt.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.action.EtcActionType;
import org.example.sharedprompts.domain.prompt.enums.role.EtcRoleType;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.enums.serializer.RoleTypeDeserializer;
import org.example.sharedprompts.domain.prompt.enums.serializer.RoleTypeSerializer;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptCommand;

import java.util.List;

/**
 * 프롬프트 생성 요청 DTO (adapter/in/web 전용).
 * 사용자 입력만 담는다. Objective/Strategy/QualityPriority 등 내부 엔진 개념은 노출하지 않는다.
 */
public record GeneratePromptRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 200, message = "제목은 최대 200자까지 입력해주세요.")
        String title,

        @NotBlank(message = "설명을 입력해주세요.")
        @Size(max = 5000, message = "설명은 최대 5000자까지 입력해주세요.")
        String description,

        @JsonProperty("is_public")
        Boolean isPublic,

        @NotNull(message = "카테고리를 입력해주세요.")
        @JsonProperty("prompt_category")
        PromptCategory promptCategory,

        List<@Size(min = 1, max = 50, message = "태그는 1~50자로 입력해주세요.") String> tags,

        @NotBlank(message = "입력값을 입력해주세요.")
        @Size(max = 10000, message = "입력은 최대 10000자까지 입력해주세요.")
        String input,

        @JsonProperty("action_type")
        @JsonSerialize(using = ActionTypeSerializer.class)
        @JsonDeserialize(using = ActionTypeDeserializer.class)
        ActionTypeInterface actionType,

        @JsonProperty("role_type")
        @JsonSerialize(using = RoleTypeSerializer.class)
        @JsonDeserialize(using = RoleTypeDeserializer.class)
        RoleTypeInterface roleType,

        ToneType tone,
        StyleType style,
        LanguageType language,

        @JsonProperty("json_schema")
        String jsonSchema
) {
    /** 커맨드 변환 */
    public GeneratePromptCommand toCommand(Long userId) {
        return new GeneratePromptCommand(
                userId,
                title,
                description,
                isPublic != null ? isPublic : false,
                promptCategory,
                tags != null ? List.copyOf(tags) : List.of(),
                input,
                actionType != null ? actionType : EtcActionType.GENERAL_CONSULTATION,
                roleType != null ? roleType : EtcRoleType.GENERAL_CONSULTANT,
                tone != null ? tone : ToneType.NEUTRAL,
                style != null ? style : StyleType.NARRATIVE,
                language != null ? language : LanguageType.KOREAN,
                false,  // experimental: 기본 비활성화
                jsonSchema
        );
    }
}
