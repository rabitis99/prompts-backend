package org.example.sharedprompts.domain.prompt.application.semantic.observability;

import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationMetricsEvent;
import java.util.List;

/**
 * Builds metrics event from result via assembler and emits to all configured sinks.
 * Service does not know about event shape or sink implementation.
 */
public class DefaultRecommendationObservabilityPublisher implements RecommendationObservabilityPublisher {

    private final RecommendationMetricsAssembler assembler;
    private final List<RecommendationMetricsSink> sinks;

    public DefaultRecommendationObservabilityPublisher(
            RecommendationMetricsAssembler assembler,
            List<RecommendationMetricsSink> sinks
    ) {
        this.assembler = assembler != null ? assembler : new DefaultRecommendationMetricsAssembler();
        this.sinks = sinks != null ? List.copyOf(sinks) : List.of();
    }

    @Override
    public void publish(RecommendPromptResult result) {
        if (result == null) {
            return;
        }
        RecommendationMetricsEvent event = assembler.toMetricsEvent(result);
        for (RecommendationMetricsSink sink : sinks) {
            try {
                sink.emit(event);
            } catch (Exception e) {
                // Sink must not throw; if it does, log and continue so one bad sink does not break others
                // In production a proper logger would be used
            }
        }
    }
}
