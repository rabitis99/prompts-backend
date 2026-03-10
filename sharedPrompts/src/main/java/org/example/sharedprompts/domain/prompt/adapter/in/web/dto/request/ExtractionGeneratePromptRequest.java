package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.validator.RequestTypeMustBe;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.ExpressionOptions;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.OutputOptions;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.RequestType;

import java.util.List;

/**
 * JSON Schema 기반 데이터 추출/구조화 요청 DTO
 */
public record ExtractionGeneratePromptRequest(

        @JsonProperty("request_type")
        @NotNull(message = "request_type을 입력해주세요.")
        @RequestTypeMustBe(value = RequestType.EXTRACTION, message = "request_type은 EXTRACTION이어야 합니다.")
        RequestType requestType,

        @NotBlank(message = "입력을 입력해주세요.")
        @Size(max = 10_000, message = "입력은 최대 10000자까지 입력해주세요.")
        String input,

        @NotBlank(message = "JSON Schema를 입력해주세요.")
        @Size(max = 20_000, message = "JSON Schema는 최대 20000자까지 입력해주세요.")
        @JsonProperty("json_schema")
        String jsonSchema,

        LanguageType language,

        @Size(max = 20, message = "태그는 최대 20개까지 가능합니다.")
        List<
                @NotBlank(message = "태그는 공백일 수 없습니다.")
                @Size(max = 50, message = "태그는 1~50자로 입력해주세요.")
                        String> tags,

        @Size(max = 200, message = "제목은 최대 200자까지 입력해주세요.")
        String title,

        @Size(max = 65_535, message = "설명은 최대 65535자까지 입력해주세요.")
        String description
) implements UnifiedGeneratePromptRequest {

    @Override
    public UnifiedGeneratePromptCommand toCommand(Long userId) {

        ExpressionOptions expression = new ExpressionOptions(
                null,
                null,
                language,
                null
        );

        OutputOptions output = new OutputOptions(
                jsonSchema,
                null
        );

        return UnifiedGeneratePromptCommand.fromNormalized(
                userId,
                RequestMode.EXTRACTION,
                null, // 추출 모드는 semantic 축을 사용하지 않음
                expression,
                output,
                null,
                input,
                false,
                tags,
                normalizeOptional(title),
                normalizeOptional(description)
        );
    }

    /**
     * 선택값이 빈 문자열이면 null로 변환하여 기본값 처리 가능하게 함
     */
    private static String normalizeOptional(String value) {
        return (value != null && !value.isBlank()) ? value : null;
    }
}