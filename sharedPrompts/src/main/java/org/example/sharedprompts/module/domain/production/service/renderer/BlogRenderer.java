package org.example.sharedprompts.module.domain.production.service.renderer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

/**
 * Blog Renderer
 * 
 * <p>Blog Command의 파싱된 응답을 Markdown 포맷으로 렌더링
 */
@Component
@Slf4j
public class BlogRenderer implements ProductionRenderer {

    @Override
    public String render(JsonNode parsedResponse) {
        log.debug("Rendering blog content");

        String title = parsedResponse.has("title") 
                ? parsedResponse.get("title").asText() 
                : "Untitled";
        String content = parsedResponse.has("content") 
                ? parsedResponse.get("content").asText() 
                : "";
        String tags = parsedResponse.has("tags") 
                ? parsedResponse.get("tags").toString() 
                : "[]";

        // Markdown 포맷으로 렌더링
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(title).append("\n\n");
        markdown.append(content).append("\n\n");
        
        if (!tags.equals("[]")) {
            markdown.append("## Tags\n");
            JsonNode tagsNode = parsedResponse.get("tags");
            if (tagsNode.isArray()) {
                for (JsonNode tag : tagsNode) {
                    markdown.append("- ").append(tag.asText()).append("\n");
                }
            }
        }

        return markdown.toString();
    }

    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.BLOG;
    }
}

