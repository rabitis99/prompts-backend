package org.example.sharedprompts.domain.prompt.application.semantic.observability;

import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;

/**
 * Publishes recommendation observability: builds structured event from result + trace and sends to sink(s).
 * Service/facade only calls publish; they do not construct metrics or touch sink directly.
 */
public interface RecommendationObservabilityPublisher {

    /**
     * Publish observability for a completed recommendation.
     * Extracts dimensions from result/trace and emits to configured sink(s).
     * Must not throw; failures in sink are handled by the sink.
     */
    void publish(RecommendPromptResult result);
}
