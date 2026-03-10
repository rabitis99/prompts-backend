package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.ExpressionOptions;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.OutputOptions;
import org.example.sharedprompts.domain.prompt.application.port.in.command.normalization.SemanticSelection;
import org.example.sharedprompts.domain.prompt.common.enums.*;

import java.util.List;

/**
 * 기본 프롬프트 생성 요청 DTO
 */
public record SimpleGeneratePromptRequest(

        @JsonProperty("request_type")
        @NotNull(message = "request_type을 입력해주세요.")
        RequestType requestType,

        @NotNull(message = "카테고리를 선택해주세요.")
        PromptCategory category,

        @NotNull(message = "의도(intent)를 선택해주세요.")
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
        List<
                @NotBlank(message = "태그는 공백일 수 없습니다.")
                @Size(max = 50, message = "태그는 1~50자로 입력해주세요.")
                        String> tags,

        @Size(max = 200, message = "제목은 최대 200자까지 입력해주세요.")
        String title,

        @Size(max = 65_535, message = "설명은 최대 65535자까지 입력해주세요.")
        String description
) implements UnifiedGeneratePromptRequest {

    public SimpleGeneratePromptRequest {
        // SIMPLE 요청만 허용
        if (requestType != null && requestType != RequestType.SIMPLE) {
            throw new IllegalArgumentException("request_type must be SIMPLE for SimpleGeneratePromptRequest");
        }
    }

    @Override
    public UnifiedGeneratePromptCommand toCommand(Long userId) {

        // role/action은 simple 모드에서 자동 추천됨
        SemanticSelection semantic = new SemanticSelection(category, intent, null, null);

        ExpressionOptions expression = new ExpressionOptions(
                tone,
                style,
                language,
                experience
        );

        OutputOptions output = new OutputOptions(
                null,
                null
        );

        return UnifiedGeneratePromptCommand.fromNormalized(
                userId,
                RequestMode.SIMPLE,
                semantic,
                expression,
                output,
                normalizeOptional(variant),
                input,
                false,
                tags,
                normalizeOptional(title),
                normalizeOptional(description)
        );
    }

    /**
     * 선택값이 빈 문자열이면 null로 변환하여 기본값 처리가 가능하도록 함
     */
    private static String normalizeOptional(String value) {
        return (value != null && !value.isBlank()) ? value : null;
    }
}