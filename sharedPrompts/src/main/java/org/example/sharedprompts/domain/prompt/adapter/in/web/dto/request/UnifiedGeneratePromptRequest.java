package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.RequestType;

import java.util.List;

/**
 * 통합 프롬프트 생성 요청의 루트 타입
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "request_type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleGeneratePromptRequest.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = ExtractionGeneratePromptRequest.class, name = "EXTRACTION"),
        @JsonSubTypes.Type(value = AdvancedGeneratePromptRequest.class, name = "ADVANCED")
})
public sealed interface UnifiedGeneratePromptRequest
        permits SimpleGeneratePromptRequest, ExtractionGeneratePromptRequest, AdvancedGeneratePromptRequest {

    /**
     * 요청 유형
     */
    @JsonProperty("request_type")
    RequestType requestType();

    /**
     * 사용자 입력
     */
    String input();

    /**
     * 태그 목록
     */
    List<String> tags();

    /**
     * 제목
     */
    String title();

    /**
     * 설명
     */
    String description();

    /**
     * API 요청을 통합 커맨드로 변환
     */
    UnifiedGeneratePromptCommand toCommand(Long userId);
}