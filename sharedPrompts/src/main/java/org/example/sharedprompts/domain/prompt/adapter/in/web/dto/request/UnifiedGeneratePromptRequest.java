package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.RequestType;

import java.util.List;

/**
 * 통합 프롬프트 생성 요청의 폴리모픽 루트 타입.
 *
 * <p>외부에는 단일 엔드포인트만 노출하지만, request_type 에 따라
 * SIMPLE / EXTRACTION / ADVANCED 서브 타입으로 디스패치된다.</p>
 *
 * <p>공통·필수 필드는 이 인터페이스에서 계약으로 관리한다.</p>
 * <ul>
 *   <li>{@link #requestType()} — 요청 유형 디스크리미네이터 (필수)</li>
 *   <li>{@link #input()} — 사용자 입력 본문 (필수, 최대 10_000자)</li>
 *   <li>{@link #tags()} — 태그 목록 (선택, 최대 20개, 각 1~50자)</li>
 * </ul>
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
     * 요청 유형 디스크리미네이터. (공통·필수)
     */
    @JsonProperty("request_type")
    RequestType requestType();

    /**
     * 사용자 입력 본문. (공통·필수, 최대 10_000자)
     */
    String input();

    /**
     * 태그 목록. (공통, 최대 20개, 각 1~50자)
     */
    List<String> tags();

    /**
     * 도메인 유스케이스에서 사용하는 통합 커맨드로 변환.
     */
    UnifiedGeneratePromptCommand toCommand(Long userId);
}
