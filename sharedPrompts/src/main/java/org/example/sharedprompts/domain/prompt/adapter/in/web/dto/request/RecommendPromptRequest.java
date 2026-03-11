package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotNull;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeSerializer;

/**
 * 프롬프트 추천 요청 DTO
 */
public record RecommendPromptRequest(

        @JsonProperty("request_mode")
        @NotNull(message = "요청 모드를 선택해주세요.")
        RequestMode requestMode,

        PromptCategory category,
        ActionIntent intent,

        @JsonProperty("role_type")
        @JsonSerialize(using = RoleTypeSerializer.class)
        @JsonDeserialize(using = RoleTypeDeserializer.class)
        RoleTypeInterface roleType,

        @JsonProperty("action_type")
        @JsonSerialize(using = ActionTypeSerializer.class)
        @JsonDeserialize(using = ActionTypeDeserializer.class)
        ActionTypeInterface actionType,

        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experience,

        @JsonProperty("raw_input")
        String rawInput
) {
}