package org.example.sharedprompts.domain.prompt.application.semantic.observability;

import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.PolicyMetricsDimensions;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationEvaluationRecord;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationEvaluationSnapshot;
import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationMetricsEvent;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10차: Observability layer — structured metrics event, publisher/sink, online vs offline separation.
 */
@DisplayName("Recommendation observability: metrics event, dimensions, evaluation record, publisher/sink")
class RecommendationObservabilityTest {

    private static RecommendationTrace traceWithPolicyAndFallback(
            String policyVersionId,
            String experimentId,
            String variantId,
            boolean fallbackIntentUsed,
            boolean orderingFallbackApplied
    ) {
        PolicyTraceInfo policyInfo = PolicyTraceInfo.of(policyVersionId, "test-source", experimentId, variantId);
        PolicyApplicationTrace policyTrace = PolicyApplicationTrace.of("in-memory", "test-source", "Ordering", orderingFallbackApplied);
        OrderingTrace orderingTrace = OrderingTrace.of(
                policyTrace,
                orderingFallbackApplied,
                List.of(
                        new OrderingTrace.OrderedItemTrace("action.1", "preference", false),
                        new OrderingTrace.OrderedItemTrace("action.2", "fallback", true)
                )
        );
        List<ActionRecommendationTrace> actionTraces = List.of(
                ActionRecommendationTrace.of("action.1", "compatibility", policyTrace),
                ActionRecommendationTrace.of("action.2", "compatibility", policyTrace)
        );
        List<RoleRecommendationTrace> roleTraces = List.of(
                RoleRecommendationTrace.of("role.1", "profile", policyTrace)
        );
        return RecommendationTrace.builder(PromptCategory.WRITING, ActionIntent.GENERATE, fallbackIntentUsed)
                .policyTraceInfo(policyInfo)
                .actionOrderingTrace(orderingTrace)
                .actionTraces(actionTraces)
                .roleTraces(roleTraces)
                .build();
    }

    private static RecommendPromptResult resultWithTrace(
            String policyVersionId,
            String experimentId,
            String variantId,
            boolean fallbackIntentUsed,
            boolean orderingFallback
    ) {
        RecommendationTrace trace = traceWithPolicyAndFallback(
                policyVersionId, experimentId, variantId, fallbackIntentUsed, orderingFallback
        );
        return new RecommendPromptResult(
                RequestMode.ADVANCED,
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                List.of(ActionIntent.GENERATE),
                null,
                List.of(),
                null,
                List.of(),
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                null,
                List.of(),
                List.of(),
                fallbackIntentUsed ? List.of("intent: profile fallback applied") : List.of(),
                "GENERATE",
                Optional.of(trace)
        );
    }

    @Test
    @DisplayName("Recommendation result is published as structured metrics event")
    void resultPublishedAsStructuredMetricsEvent() {
        InMemoryRecommendationMetricsSink sink = new InMemoryRecommendationMetricsSink();
        RecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendationObservabilityPublisher publisher = new DefaultRecommendationObservabilityPublisher(assembler, List.of(sink));

        RecommendPromptResult result = resultWithTrace("v1", "exp1", "varA", false, false);
        publisher.publish(result);

        assertThat(sink.getEvents()).hasSize(1);
        RecommendationMetricsEvent event = sink.getEvents().get(0);
        assertThat(event.categoryKey()).isEqualTo("WRITING");
        assertThat(event.intentKey()).isEqualTo("GENERATE");
        assertThat(event.policyVersion()).isEqualTo("v1");
        assertThat(event.candidateActionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Metrics event includes policyVersion, experimentId, variantId")
    void metricsEventIncludesPolicyVersionExperimentVariant() {
        DefaultRecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendPromptResult result = resultWithTrace("policy-2026-03", "exp-42", "control", false, false);

        RecommendationMetricsEvent event = assembler.toMetricsEvent(result);

        assertThat(event.policyVersion()).isEqualTo("policy-2026-03");
        assertThat(event.experimentId()).isEqualTo("exp-42");
        assertThat(event.variantId()).isEqualTo("control");
        assertThat(event.dimensions().policyVersion()).isEqualTo("policy-2026-03");
        assertThat(event.dimensions().experimentId()).isEqualTo("exp-42");
        assertThat(event.dimensions().variantId()).isEqualTo("control");
    }

    @Test
    @DisplayName("When fallback is applied, metrics event has fallbackApplied true")
    void fallbackAppliedReflectedInMetricsEvent() {
        DefaultRecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendPromptResult resultWithIntentFallback = resultWithTrace("v1", null, null, true, false);
        RecommendPromptResult resultWithOrderingFallback = resultWithTrace("v1", null, null, false, true);
        RecommendPromptResult resultNoFallback = resultWithTrace("v1", null, null, false, false);

        assertThat(assembler.toMetricsEvent(resultWithIntentFallback).fallbackApplied()).isTrue();
        assertThat(assembler.toMetricsEvent(resultWithOrderingFallback).fallbackApplied()).isTrue();
        assertThat(assembler.toMetricsEvent(resultNoFallback).fallbackApplied()).isFalse();
    }

    @Test
    @DisplayName("Online metrics event and offline evaluation record are separate structures")
    void onlineMetricsAndOfflineEvaluationRecordAreSeparate() {
        DefaultRecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendPromptResult result = resultWithTrace("v1", "e1", "v1", false, false);

        RecommendationMetricsEvent onlineEvent = assembler.toMetricsEvent(result);
        RecommendationEvaluationRecord offlineRecord = assembler.toEvaluationRecord(result);

        assertThat(onlineEvent).isNotNull();
        assertThat(offlineRecord).isNotNull();
        assertThat(onlineEvent).isNotEqualTo(offlineRecord);
        assertThat(onlineEvent.candidateActionCount()).isEqualTo(0);
        assertThat(onlineEvent.fallbackApplied()).isFalse();
        assertThat(offlineRecord.expectedActionKey()).isEmpty();
        assertThat(offlineRecord.expectedRoleKey()).isEmpty();
        assertThat(offlineRecord.policyVersion()).isEqualTo(onlineEvent.policyVersion());
    }

    @Test
    @DisplayName("Publisher extracts dimensions from trace/audit into event only")
    void publisherExtractsDimensionsFromTraceIntoEvent() {
        DefaultRecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendPromptResult result = resultWithTrace("v2", "exp2", "treatment", true, true);

        RecommendationMetricsEvent event = assembler.toMetricsEvent(result);

        assertThat(event.categoryKey()).isEqualTo("WRITING");
        assertThat(event.intentKey()).isEqualTo("GENERATE");
        assertThat(event.policyVersion()).isEqualTo("v2");
        assertThat(event.experimentId()).isEqualTo("exp2");
        assertThat(event.variantId()).isEqualTo("treatment");
        assertThat(event.fallbackApplied()).isTrue();
        PolicyMetricsDimensions dims = event.dimensions();
        assertThat(dims.categoryKey()).isEqualTo("WRITING");
        assertThat(dims.intentKey()).isEqualTo("GENERATE");
        assertThat(dims.policyVersion()).isEqualTo("v2");
        assertThat(dims.fallbackApplied()).isTrue();
    }

    @Test
    @DisplayName("Sink implementation can be swapped without changing service")
    void sinkSwapWithoutServiceChange() {
        InMemoryRecommendationMetricsSink sink1 = new InMemoryRecommendationMetricsSink();
        InMemoryRecommendationMetricsSink sink2 = new InMemoryRecommendationMetricsSink();
        RecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendationObservabilityPublisher publisherWithSink1 = new DefaultRecommendationObservabilityPublisher(assembler, List.of(sink1));
        RecommendationObservabilityPublisher publisherWithSink2 = new DefaultRecommendationObservabilityPublisher(assembler, List.of(sink2));

        RecommendPromptResult result = resultWithTrace("v1", null, null, false, false);
        publisherWithSink1.publish(result);
        assertThat(sink1.getEvents()).hasSize(1);
        assertThat(sink2.getEvents()).isEmpty();

        publisherWithSink2.publish(result);
        assertThat(sink2.getEvents()).hasSize(1);
    }

    @Test
    @DisplayName("Category, intent, action, role axes are present in metrics dimensions")
    void categoryIntentActionRoleInDimensions() {
        DefaultRecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendPromptResult result = resultWithTrace("v1", null, null, false, false);

        RecommendationMetricsEvent event = assembler.toMetricsEvent(result);
        PolicyMetricsDimensions d = event.dimensions();

        assertThat(d.categoryKey()).isEqualTo("WRITING");
        assertThat(d.intentKey()).isEqualTo("GENERATE");
        assertThat(d.dimensionValues()).contains("WRITING", "GENERATE");
        assertThat(d.dimensionValues()).hasSize(9);
    }

    @Test
    @DisplayName("Policy version change yields different event dimensions")
    void policyVersionChangeYieldsDifferentDimensions() {
        DefaultRecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendPromptResult resultV1 = resultWithTrace("v1", null, null, false, false);
        RecommendPromptResult resultV2 = resultWithTrace("v2", null, null, false, false);

        RecommendationMetricsEvent e1 = assembler.toMetricsEvent(resultV1);
        RecommendationMetricsEvent e2 = assembler.toMetricsEvent(resultV2);

        assertThat(e1.policyVersion()).isEqualTo("v1");
        assertThat(e2.policyVersion()).isEqualTo("v2");
        assertThat(e1.dimensions().policyVersion()).isNotEqualTo(e2.dimensions().policyVersion());
    }

    @Test
    @DisplayName("Experiment variant difference yields different aggregation key")
    void experimentVariantYieldsDifferentAggregationKey() {
        DefaultRecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendPromptResult control = resultWithTrace("v1", "exp1", "control", false, false);
        RecommendPromptResult treatment = resultWithTrace("v1", "exp1", "treatment", false, false);

        RecommendationMetricsEvent eControl = assembler.toMetricsEvent(control);
        RecommendationMetricsEvent eTreatment = assembler.toMetricsEvent(treatment);

        assertThat(eControl.variantId()).isEqualTo("control");
        assertThat(eTreatment.variantId()).isEqualTo("treatment");
        assertThat(eControl.dimensions().dimensionValues()).isNotEqualTo(eTreatment.dimensions().dimensionValues());
    }

    @Test
    @DisplayName("Evaluation snapshot can be created without expected label")
    void evaluationSnapshotWithoutExpectedLabel() {
        DefaultRecommendationMetricsAssembler assembler = new DefaultRecommendationMetricsAssembler();
        RecommendPromptResult result = resultWithTrace("v1", null, null, false, false);
        RecommendationEvaluationRecord record = assembler.toEvaluationRecord(result);

        RecommendationEvaluationSnapshot snapshot = RecommendationEvaluationSnapshot.single("snap-1", record);

        assertThat(snapshot.records()).hasSize(1);
        assertThat(snapshot.records().get(0).expectedActionKey()).isEmpty();
        assertThat(snapshot.records().get(0).expectedRoleKey()).isEmpty();
        assertThat(snapshot.snapshotAt()).isNotNull();
    }
}
