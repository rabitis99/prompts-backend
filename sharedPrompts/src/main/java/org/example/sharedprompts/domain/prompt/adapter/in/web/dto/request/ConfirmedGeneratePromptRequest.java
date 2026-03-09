package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeSerializer;

import java.util.List;

/**
 * Request for POST /prompts/generate/confirmed.
 * objective and output_needs are derived from intent in the backend (IntentDictionary).
 */
public record ConfirmedGeneratePromptRequest(
        @JsonProperty("request_mode")
        @NotNull
        RequestMode requestMode,

        @NotNull
        PromptCategory category,

        @NotNull
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

        @NotBlank
        @Size(max = 10_000)
        String input,

        @JsonProperty("json_schema")
        @Size(max = 20_000)
        String jsonSchema,

        @Size(max = 200)
        String title,

        @Size(max = 5_000)
        String description,

        @Size(max = 20)
        List<@Size(max = 50) String> tags
) {
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
