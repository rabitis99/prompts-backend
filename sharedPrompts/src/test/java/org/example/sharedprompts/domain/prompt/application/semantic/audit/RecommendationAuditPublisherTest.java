package org.example.sharedprompts.domain.prompt.application.semantic.audit;

import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.PolicyTraceInfo;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationAuditRecord;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit record is created and passed to sink on production path (facade calls publisher with result).
 */
@DisplayName("Recommendation audit publisher")
class RecommendationAuditPublisherTest {

    @Test
    @DisplayName("publish builds audit record from result and forwards to sink")
    void publishBuildsRecordAndForwardsToSink() {
        List<RecommendationAuditRecord> received = new ArrayList<>();
        RecommendationAuditSink sink = received::add;
        RecommendationAuditPublisher publisher = new DefaultRecommendationAuditPublisher(List.of(sink));

        RecommendPromptResult result = new RecommendPromptResult(
                RequestMode.ADVANCED, PromptCategory.WRITING, ActionIntent.GENERATE,
                List.of(), null, List.of(), null, List.of(),
                null, null, null, List.of(), List.of(), List.of(), null,
                Optional.empty()
        );

        publisher.publish(result);

        assertThat(received).hasSize(1);
        RecommendationAuditRecord record = received.get(0);
        assertThat(record.categoryKey()).isEqualTo("WRITING");
        assertThat(record.intentKey()).isEqualTo("GENERATE");
        assertThat(record.traceSummary()).isNull();
    }

    @Test
    @DisplayName("publish with trace builds record with trace snapshot")
    void publishWithTraceIncludesTraceSnapshot() {
        List<RecommendationAuditRecord> received = new ArrayList<>();
        RecommendationAuditSink sink = received::add;
        RecommendationAuditPublisher publisher = new DefaultRecommendationAuditPublisher(List.of(sink));

        RecommendationTrace trace = RecommendationTrace.builder(PromptCategory.WRITING, ActionIntent.GENERATE, false)
                .policyTraceInfo(PolicyTraceInfo.of("2026-03-recommendation-v1", null))
                .build();
        RecommendPromptResult result = new RecommendPromptResult(
                RequestMode.ADVANCED, PromptCategory.WRITING, ActionIntent.GENERATE,
                List.of(), null, List.of(), null, List.of(),
                null, null, null, List.of(), List.of(), List.of(), null,
                Optional.of(trace)
        );

        publisher.publish(result);

        assertThat(received).hasSize(1);
        RecommendationAuditRecord record = received.get(0);
        assertThat(record.traceSummary()).isNotNull();
        assertThat(record.traceSummary().policyVersionId()).isEqualTo("2026-03-recommendation-v1");
    }
}
