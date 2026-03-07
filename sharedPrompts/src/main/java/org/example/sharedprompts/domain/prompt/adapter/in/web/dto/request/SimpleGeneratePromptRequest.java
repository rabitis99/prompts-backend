package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.*;

import java.util.List;

/**
 * 기본 단순 프롬프트 생성 요청.
 */
public record SimpleGeneratePromptRequest(

        @JsonProperty("request_type")
        RequestType requestType,

        PromptCategory category,
        ActionIntent intent,
        String variant,

        @NotBlank(message = "입력을 입력해주세요.")
        @Size(max = 10_000, message = "입력은 최대 10000자까지 입력해주세요.")
        String input,

        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experience,

        @Size(max = 20, message = "태그는 최대 20개까지 가능합니다.")
        List<@NotBlank(message = "태그는 공백일 수 없습니다.")
                @Size(max = 50, message = "태그는 1~50자로 입력해주세요.")
                String> tags,

        @Size(max = 200, message = "제목은 최대 200자까지 입력해주세요.")
        String title,

        @Size(max = 65_535, message = "설명은 최대 65535자까지 입력해주세요.")
        String description
) implements UnifiedGeneratePromptRequest {

    public SimpleGeneratePromptRequest {
        if (requestType != RequestType.SIMPLE) {
            throw new IllegalArgumentException("request_type must be SIMPLE for SimpleGeneratePromptRequest");
        }
    }

    @Override
    public UnifiedGeneratePromptCommand toCommand(Long userId) {
        return UnifiedGeneratePromptCommand.forSimple(
                userId,
                category,
                intent,
                variant,
                input,
                tone,
                style,
                language,
                experience,
                tags,
                normalizeOptional(title),
                normalizeOptional(description)
        );
    }

    /** API에서는 선택값인 title/description을 빈 문자열이 아닌 null로 내려보내 downstream fallback이 적용되도록 함. */
    private static String normalizeOptional(String value) {
        return (value != null && !value.isBlank()) ? value : null;
    }
}

