package org.example.sharedprompts.domain.prompt.application.semantic.observability;

import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationMetricsEvent;
/**
 * No-op sink for when no metrics backend is configured.
 * Allows observability pipeline to be always present without side effects.
 */
public class NoOpRecommendationMetricsSink implements RecommendationMetricsSink {

    @Override
    public void emit(RecommendationMetricsEvent event) {
        // no-op
    }
}
