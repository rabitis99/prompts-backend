package org.example.sharedprompts.module.domain.production.service.renderer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

/**
 * Document Renderer
 * 
 * <p>Document Command의 파싱된 응답을 Markdown 포맷으로 렌더링
 */
@Component
@Slf4j
public class DocumentRenderer implements ProductionRenderer {

    @Override
    public String render(JsonNode parsedResponse) {
        log.debug("Rendering document content");

        String content = parsedResponse.has("content") 
                ? parsedResponse.get("content").asText() 
                : "";

        // Markdown 포맷으로 렌더링 (간단한 구현)
        return content;
    }

    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.DOCUMENT;
    }
}

