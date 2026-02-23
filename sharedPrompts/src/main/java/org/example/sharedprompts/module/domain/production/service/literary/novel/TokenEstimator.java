package org.example.sharedprompts.module.domain.production.service.literary.novel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenEstimator {

    private static final int DEFAULT_CHARS_PER_TOKEN = 3;

    @Value("${production.literary.novel.max-tokens-per-request:4000}")
    private int maxTokensPerRequest = 4000;

    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (text.length() + DEFAULT_CHARS_PER_TOKEN - 1) / DEFAULT_CHARS_PER_TOKEN;
    }

    public int getMaxTokensPerRequest() {
        return maxTokensPerRequest;
    }

    public boolean fitsInSingleRequest(String text) {
        return estimateTokens(text) <= maxTokensPerRequest;
    }
}
