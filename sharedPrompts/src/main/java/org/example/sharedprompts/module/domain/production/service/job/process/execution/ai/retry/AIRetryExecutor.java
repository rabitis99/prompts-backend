package org.example.sharedprompts.module.domain.production.service.job.process.execution.ai.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentRequest;
import org.example.sharedprompts.module.domain.production.model.ai.AIContentResult;
import org.example.sharedprompts.module.domain.production.service.ai.AIService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AIRetryExecutor {

    private final AIRetryPolicy retryPolicy;
    private final AIErrorClassifier errorClassifier;

    private static final int MAX_AI_RETRY_COUNT = 2;

    public AIContentResult executeWithRetry(AIService aiService, AIContentRequest request) {
        int retryCount = 0;
        Exception lastException = null;
        AIContentResult lastFailureResult = null;

        while (retryCount <= MAX_AI_RETRY_COUNT) {
            try {
                AIContentResult result = aiService.generateContent(request);

                if (result.isSuccess()) {
                    return result;
                }

                lastFailureResult = result;
                String errorMessage = result.getErrorMessage();

                if (errorClassifier.isPermanentError(errorMessage)) {
                    log.warn("AI call failed with permanent error, not retrying: {}", errorMessage);
                    return result;
                }

                retryCount++;
                if (retryCount <= MAX_AI_RETRY_COUNT) {
                    long backoffMs = retryPolicy.calculateBackoff(retryCount);
                    log.warn("AI call failed, retrying - attempt: {}/{}, backoff: {}ms, error: {}",
                            retryCount, MAX_AI_RETRY_COUNT, backoffMs, errorMessage);
                    retryPolicy.sleep(backoffMs);
                }

            } catch (Exception e) {
                lastException = e;
                retryCount++;
                if (retryCount <= MAX_AI_RETRY_COUNT) {
                    long backoffMs = retryPolicy.calculateBackoff(retryCount);
                    log.warn("AI call threw exception, retrying - attempt: {}/{}, backoff: {}ms",
                            retryCount, MAX_AI_RETRY_COUNT, backoffMs, e);
                    retryPolicy.sleep(backoffMs);
                }
            }
        }

        log.error("AI call failed after {} retries", MAX_AI_RETRY_COUNT, lastException);
        if (lastFailureResult != null) {
            return lastFailureResult;
        }
        return AIContentResult.failure("AI call failed after " + MAX_AI_RETRY_COUNT + " retries: " +
                (lastException != null ? lastException.getMessage() : "Unknown error"));
    }
}

