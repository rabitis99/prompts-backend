package org.example.sharedprompts.module.domain.production.service.renderer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

/**
 * Email Renderer
 * 
 * <p>Email Command의 파싱된 응답을 HTML 포맷으로 렌더링
 */
@Component
@Slf4j
public class EmailRenderer implements ProductionRenderer {

    @Override
    public String render(JsonNode parsedResponse) {
        log.debug("Rendering email content");

        String subject = parsedResponse.has("subject") 
                ? parsedResponse.get("subject").asText() 
                : "No Subject";
        String body = parsedResponse.has("body") 
                ? parsedResponse.get("body").asText() 
                : "";
        String to = parsedResponse.has("to") 
                ? parsedResponse.get("to").asText() 
                : "";

        // HTML 포맷으로 렌더링
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <title>").append(escapeHtml(subject)).append("</title>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <h1>").append(escapeHtml(subject)).append("</h1>\n");
        if (!to.isEmpty()) {
            html.append("  <p><strong>To:</strong> ").append(escapeHtml(to)).append("</p>\n");
        }
        html.append("  <div>\n");
        html.append("    ").append(body.replace("\n", "<br>\n    ")).append("\n");
        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.EMAIL;
    }

    /**
     * HTML 이스케이프
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

