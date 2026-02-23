package org.example.sharedprompts.module.domain.production.service.literary;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.service.parser.AIResponseParser;
import org.example.sharedprompts.module.domain.production.service.parser.ParsedResponse;
import org.springframework.stereotype.Component;

@Component
public class LiteraryResponseExtractor {

    private final AIResponseParser aiResponseParser;

    public LiteraryResponseExtractor(AIResponseParser aiResponseParser) {
        this.aiResponseParser = aiResponseParser;
    }

    public String extractContent(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("{") && trimmed.contains("content")) {
            try {
                ParsedResponse parsed = aiResponseParser.parseResponse(rawResponse, ProductionCommandType.TEXT);
                if (parsed != null && parsed.jsonNode() != null && parsed.jsonNode().has("content")) {
                    return parsed.jsonNode().get("content").asText("");
                }
            } catch (Exception ignored) {
            }
        }
        return trimmed;
    }
}
