package org.example.sharedprompts.module.domain.production.service.renderer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

/**
 * Text Renderer
 * 
 * <p>Text Command의 파싱된 응답을 plain text 포맷으로 렌더링
 */
@Component
@Slf4j
public class TextRenderer implements ProductionRenderer {

    @Override
    public String render(JsonNode parsedResponse) {
        log.debug("Rendering text content");

        // content 필드에서 텍스트 추출
        String content = parsedResponse.has("content") 
                ? parsedResponse.get("content").asText() 
                : "";

        // content 필드가 없으면 전체 JSON을 텍스트로 변환
        if (content.isEmpty() && parsedResponse.isTextual()) {
            content = parsedResponse.asText();
        } else if (content.isEmpty() && parsedResponse.isObject()) {
            // 객체인 경우 첫 번째 필드의 값을 사용하거나 전체를 문자열로 변환
            content = parsedResponse.toString();
        }

        // Plain text로 반환
        return content;
    }

    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.TEXT;
    }
}

