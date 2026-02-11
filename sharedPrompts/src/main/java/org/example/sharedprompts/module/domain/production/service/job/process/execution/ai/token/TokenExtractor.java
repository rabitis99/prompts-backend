package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.token;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TokenExtractor {

    public String extractTokenUsage(AIContentResult aiResult) {
        try {
            Long promptTokens = aiResult.getPromptTokens();
            Long completionTokens = aiResult.getCompletionTokens();
            Long totalTokens = aiResult.getTotalTokens();

            if (totalTokens != null) {
                return String.format("{\"promptTokens\": %d, \"completionTokens\": %d, \"totalTokens\": %d}",
                        promptTokens != null ? promptTokens : 0,
                        completionTokens != null ? completionTokens : 0,
                        totalTokens);
            }

            return "{\"promptTokens\": 0, \"completionTokens\": 0, \"totalTokens\": 0}";
        } catch (Exception e) {
            log.warn("Failed to extract token usage", e);
            return "{\"promptTokens\": 0, \"completionTokens\": 0, \"totalTokens\": 0}";
        }
    }
}

