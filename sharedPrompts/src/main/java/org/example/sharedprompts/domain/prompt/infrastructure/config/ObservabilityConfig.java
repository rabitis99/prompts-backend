package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.application.semantic.observability.DefaultRecommendationObservabilityPublisher;
import org.example.sharedprompts.domain.prompt.application.semantic.observability.RecommendationMetricsAssembler;
import org.example.sharedprompts.domain.prompt.application.semantic.observability.RecommendationMetricsSink;
import org.example.sharedprompts.domain.prompt.application.semantic.observability.RecommendationObservabilityPublisher;
import org.example.sharedprompts.domain.prompt.application.semantic.observability.NoOpRecommendationMetricsSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Observability for recommendation: publisher + default sink.
 * Replace or add RecommendationMetricsSink beans to plug in Prometheus/Kafka/etc. later.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    @ConditionalOnMissingBean(RecommendationMetricsSink.class)
    public RecommendationMetricsSink noOpRecommendationMetricsSink() {
        return new NoOpRecommendationMetricsSink();
    }

    @Bean
    public RecommendationObservabilityPublisher recommendationObservabilityPublisher(
            RecommendationMetricsAssembler assembler,
            List<RecommendationMetricsSink> sinks
    ) {
        return new DefaultRecommendationObservabilityPublisher(assembler, sinks);
    }
}
