package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.RequestType;

import java.util.List;

/**
 * JSON Schema 기반 추출/구조화 모드 요청.
 */
public record ExtractionGeneratePromptRequest(

        @JsonProperty("request_type")
        RequestType requestType,

        @NotBlank(message = "입력을 입력해주세요.")
        @Size(max = 10_000, message = "입력은 최대 10000자까지 입력해주세요.")
        String input,

        @NotBlank(message = "JSON Schema를 입력해주세요.")
        @Size(max = 20_000, message = "JSON Schema는 최대 20000자까지 허용됩니다.")
        @JsonProperty("json_schema")
        String jsonSchema,

        LanguageType language,

        @Size(max = 20, message = "태그는 최대 20개까지 가능합니다.")
        List<@Size(min = 1, max = 50, message = "태그는 1~50자로 입력해주세요.") String> tags,

        @Size(max = 200, message = "제목은 최대 200자까지 입력해주세요.")
        String title,

        @Size(max = 65_535, message = "설명은 최대 65535자까지 입력해주세요.")
        String description
) implements UnifiedGeneratePromptRequest {

    public ExtractionGeneratePromptRequest {
        if (requestType != RequestType.EXTRACTION) {
            throw new IllegalArgumentException("request_type must be EXTRACTION for ExtractionGeneratePromptRequest");
        }
    }

    @Override
    public UnifiedGeneratePromptCommand toCommand(Long userId) {
        return UnifiedGeneratePromptCommand.of(
                userId,
                null,          // category
                null,          // intent
                null,          // variant
                input,
                jsonSchema,
                null,          // engineMode
                null,          // tone
                null,          // style
                language,
                null,          // experience
                false,         // disableQualityPipeline
                null,          // actionType
                null,          // roleType
                null,          // coreRole
                null,          // domainRole
                tags,
                title,
                description
        );
    }
}

