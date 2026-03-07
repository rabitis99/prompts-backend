package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.RequestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 통합 프롬프트 요청 DTO의 JSON 역직렬화 및 toCommand() 변환이 실제로 동작하는지 검증.
 */
@DisplayName("UnifiedGeneratePromptRequest 역직렬화 및 toCommand 검증")
class UnifiedGeneratePromptRequestDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("SIMPLE")
    class SimpleRequest {

        @Test
        @DisplayName("JSON → SimpleGeneratePromptRequest 역직렬화 후 toCommand 성공")
        void deserialize_and_toCommand() throws Exception {
            String json = """
                    {
                      "request_type": "SIMPLE",
                      "category": "ETC",
                      "intent": "GENERATE",
                      "input": "간단한 요약 부탁해",
                      "tone": "NEUTRAL",
                      "language": "KOREAN",
                      "tags": ["요약", "간단"],
                      "title": "제목",
                      "description": "설명"
                    }
                    """;

            UnifiedGeneratePromptRequest request = objectMapper.readValue(json, UnifiedGeneratePromptRequest.class);

            assertThat(request).isInstanceOf(SimpleGeneratePromptRequest.class);
            assertThat(request.requestType()).isEqualTo(RequestType.SIMPLE);
            assertThat(request.input()).isEqualTo("간단한 요약 부탁해");
            assertThat(request.tags()).containsExactly("요약", "간단");

            UnifiedGeneratePromptCommand command = request.toCommand(100L);
            assertThat(command.userId()).isEqualTo(100L);
            assertThat(command.input()).isEqualTo("간단한 요약 부탁해");
            assertThat(command.category()).isEqualTo(PromptCategory.ETC);
            assertThat(command.intent()).isEqualTo(ActionIntent.GENERATE);
            assertThat(command.tags()).containsExactly("요약", "간단");
            assertThat(command.title()).isEqualTo("제목");
            assertThat(command.description()).isEqualTo("설명");
        }
    }

    @Nested
    @DisplayName("EXTRACTION")
    class ExtractionRequest {

        @Test
        @DisplayName("JSON → ExtractionGeneratePromptRequest 역직렬화 후 toCommand 성공")
        void deserialize_and_toCommand() throws Exception {
            String json = """
                    {
                      "request_type": "EXTRACTION",
                      "input": "본문 텍스트",
                      "json_schema": "{\\"type\\":\\"object\\"}",
                      "language": "KOREAN",
                      "tags": ["추출"],
                      "title": "추출 제목",
                      "description": "추출 설명"
                    }
                    """;

            UnifiedGeneratePromptRequest request = objectMapper.readValue(json, UnifiedGeneratePromptRequest.class);

            assertThat(request).isInstanceOf(ExtractionGeneratePromptRequest.class);
            assertThat(request.requestType()).isEqualTo(RequestType.EXTRACTION);
            assertThat(request.input()).isEqualTo("본문 텍스트");
            assertThat(request.tags()).containsExactly("추출");

            UnifiedGeneratePromptCommand command = request.toCommand(200L);
            assertThat(command.userId()).isEqualTo(200L);
            assertThat(command.input()).isEqualTo("본문 텍스트");
            assertThat(command.jsonSchema()).isEqualTo("{\"type\":\"object\"}");
            assertThat(command.language()).isEqualTo(LanguageType.KOREAN);
            assertThat(command.tags()).containsExactly("추출");
            assertThat(command.title()).isEqualTo("추출 제목");
            assertThat(command.description()).isEqualTo("추출 설명");
        }
    }

    @Nested
    @DisplayName("ADVANCED")
    class AdvancedRequest {

        @Test
        @DisplayName("JSON → AdvancedGeneratePromptRequest 역직렬화 후 toCommand 성공")
        void deserialize_and_toCommand() throws Exception {
            String json = """
                    {
                      "request_type": "ADVANCED",
                      "category": "DEVELOPMENT",
                      "intent": "CODE",
                      "input": "API 예제 코드 생성",
                      "tags": ["코드", "API"],
                      "title": "고급 제목",
                      "description": "고급 설명"
                    }
                    """;

            UnifiedGeneratePromptRequest request = objectMapper.readValue(json, UnifiedGeneratePromptRequest.class);

            assertThat(request).isInstanceOf(AdvancedGeneratePromptRequest.class);
            assertThat(request.requestType()).isEqualTo(RequestType.ADVANCED);
            assertThat(request.input()).isEqualTo("API 예제 코드 생성");
            assertThat(request.tags()).containsExactly("코드", "API");

            UnifiedGeneratePromptCommand command = request.toCommand(300L);
            assertThat(command.userId()).isEqualTo(300L);
            assertThat(command.input()).isEqualTo("API 예제 코드 생성");
            assertThat(command.category()).isEqualTo(PromptCategory.DEVELOPMENT);
            assertThat(command.intent()).isEqualTo(ActionIntent.CODE);
            assertThat(command.tags()).containsExactly("코드", "API");
            assertThat(command.title()).isEqualTo("고급 제목");
            assertThat(command.description()).isEqualTo("고급 설명");
        }
    }
}
