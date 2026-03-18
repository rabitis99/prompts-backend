package org.example.sharedprompts.domain.prompt.application.semantic.observability;

import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationMetricsEvent;

/**
 * Sink for recommendation metrics events.
 * Implementations may write to log, in-memory store, Prometheus, Kafka, etc.
 * Service and facade do not depend on concrete sink.
 */
public interface RecommendationMetricsSink {

    /**
     * Emit a single recommendation metrics event.
     * Implementations must not throw; failures should be logged or buffered.
     */
    void emit(RecommendationMetricsEvent event);
}
