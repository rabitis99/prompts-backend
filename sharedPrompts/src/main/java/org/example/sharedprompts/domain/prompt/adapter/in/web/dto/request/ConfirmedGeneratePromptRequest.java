package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeSerializer;

import java.util.List;

/**
 * 추천된 축을 확정하여 프롬프트를 생성하는 요청 DTO
 */
public record ConfirmedGeneratePromptRequest(

        @JsonProperty("request_mode")
        @NotNull(message = "요청 모드를 선택해주세요.")
        RequestMode requestMode,

        @NotNull(message = "카테고리를 선택해주세요.")
        PromptCategory category,

        @NotNull(message = "의도(intent)를 선택해주세요.")
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

        @NotBlank(message = "입력을 입력해주세요.")
        @Size(max = 10_000, message = "입력은 최대 10000자까지 입력해주세요.")
        String input,

        @JsonProperty("json_schema")
        @Size(max = 20_000, message = "JSON Schema는 최대 20000자까지 허용됩니다.")
        String jsonSchema,

        @Size(max = 200, message = "제목은 최대 200자까지 입력해주세요.")
        String title,

        @Size(max = 5_000, message = "설명은 최대 5000자까지 입력해주세요.")
        String description,

        @Size(max = 20, message = "태그는 최대 20개까지 가능합니다.")
        List<
                @NotBlank(message = "태그는 공백일 수 없습니다.")
                @Size(max = 50, message = "태그는 1~50자로 입력해주세요.")
                        String> tags
) {

        /**
         * API 요청 DTO를 Command로 변환
         */
        public ConfirmedGeneratePromptCommand toCommand(Long userId) {
                return new ConfirmedGeneratePromptCommand(
                        userId,
                        requestMode,
                        category,
                        intent,
                        roleType,
                        actionType,
                        tone,
                        style,
                        language,
                        experience,
                        input,
                        jsonSchema,
                        title,
                        description,
                        tags != null ? tags : List.of()
                );
        }
}