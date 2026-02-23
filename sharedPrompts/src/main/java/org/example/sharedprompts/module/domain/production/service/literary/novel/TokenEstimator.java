package org.example.sharedprompts.module.domain.production.service.literary.novel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenEstimator {

    /**
     * Approximate characters per token for estimation only. Actual ratio depends on the AI model's
     * tokenizer (e.g. BPE). English is often ~4 chars/token; Korean may be ~0.5–1 token per character.
     * Do not rely on this for precise token limits.
     */
    private static final int DEFAULT_CHARS_PER_TOKEN = 3;

    @Value("${production.literary.novel.max-tokens-per-request:4000}")
    private int maxTokensPerRequest;

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
