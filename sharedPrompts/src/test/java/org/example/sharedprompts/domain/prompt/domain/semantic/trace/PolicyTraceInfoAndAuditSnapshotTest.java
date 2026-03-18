package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit snapshot contains policyVersion + trace + result for reproducibility.
 */
@DisplayName("Audit snapshot: policyVersion + trace + result")
class PolicyTraceInfoAndAuditSnapshotTest {

    @Test
    @DisplayName("Snapshot from trace with policyTraceInfo includes policyVersionId and policySourceId")
    void snapshotIncludesPolicyVersionAndSource() {
        PolicyTraceInfo policyInfo = PolicyTraceInfo.of("2026-03-recommendation-v1", "in-memory");
        PolicyApplicationTrace policy = PolicyApplicationTrace.of("profile", "P", "C", false);
        RecommendationTrace trace = RecommendationTrace.builder(PromptCategory.WRITING, ActionIntent.GENERATE, false)
                .policyTraceInfo(policyInfo)
                .actionOrderingTrace(OrderingTrace.of(policy, false, List.of(
                        new OrderingTrace.OrderedItemTrace("article_writing", "preference", false),
                        new OrderingTrace.OrderedItemTrace("essay_writing", "preference", false)
                )))
                .actionTraces(List.of(
                        ActionRecommendationTrace.of("article_writing", "stage", policy),
                        ActionRecommendationTrace.of("essay_writing", "stage", policy)
                ))
                .roleTraces(List.of(RoleRecommendationTrace.of("content_writer", "stage", policy)))
                .build();

        RecommendationTraceSnapshot snapshot = RecommendationTraceSnapshot.from(trace);

        assertThat(snapshot.policyVersionId()).isEqualTo("2026-03-recommendation-v1");
        assertThat(snapshot.policySourceId()).isEqualTo("in-memory");
        assertThat(snapshot.actionKeysInOrder()).containsExactly("article_writing", "essay_writing");
    }

    @Test
    @DisplayName("Audit record stores policyVersion and trace summary together")
    void auditRecordStoresPolicyVersionAndTraceSummary() {
        PolicyTraceInfo policyInfo = PolicyTraceInfo.of("v1", "classpath-json", "exp-1", "variant-A");
        PolicyApplicationTrace policy = PolicyApplicationTrace.of("profile", "P", "C", false);
        RecommendationTrace trace = RecommendationTrace.builder(PromptCategory.WRITING, ActionIntent.GENERATE, false)
                .policyTraceInfo(policyInfo)
                .actionOrderingTrace(OrderingTrace.of(policy, false, List.of(
                        new OrderingTrace.OrderedItemTrace("a1", "pref", false)
                )))
                .actionTraces(List.of(ActionRecommendationTrace.of("a1", "stage", policy)))
                .roleTraces(List.of(RoleRecommendationTrace.of("r1", "stage", policy)))
                .build();

        RecommendationAuditRecord record = RecommendationAuditRecord.of(
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                "a1",
                "r1",
                List.of("a1"),
                List.of("r1"),
                trace
        );

        assertThat(record.traceSummary()).isNotNull();
        assertThat(record.traceSummary().policyVersionId()).isEqualTo("v1");
        assertThat(record.traceSummary().policySourceId()).isEqualTo("classpath-json");
        assertThat(record.traceSummary().experimentId()).isEqualTo("exp-1");
        assertThat(record.traceSummary().variantId()).isEqualTo("variant-A");
    }
}
