package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.application.semantic.audit.DefaultRecommendationAuditPublisher;
import org.example.sharedprompts.domain.prompt.application.semantic.audit.NoOpRecommendationAuditSink;
import org.example.sharedprompts.domain.prompt.application.semantic.audit.RecommendationAuditPublisher;
import org.example.sharedprompts.domain.prompt.application.semantic.audit.RecommendationAuditSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Audit for recommendation: publisher + default no-op sink.
 * Add RecommendationAuditSink beans to persist audit records.
 */
@Configuration
public class AuditConfig {

    @Bean
    @ConditionalOnMissingBean(RecommendationAuditSink.class)
    public RecommendationAuditSink noOpRecommendationAuditSink() {
        return new NoOpRecommendationAuditSink();
    }

    @Bean
    public RecommendationAuditPublisher recommendationAuditPublisher(List<RecommendationAuditSink> sinks) {
        return new DefaultRecommendationAuditPublisher(sinks);
    }
}
