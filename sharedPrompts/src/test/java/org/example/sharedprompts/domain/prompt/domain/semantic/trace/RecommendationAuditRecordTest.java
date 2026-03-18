package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit record and snapshot contain input + result + trace summary for future persistence.
 */
class RecommendationAuditRecordTest {

    @Test
    void snapshot_fromTrace_capturesSourceIdsAndOrder() {
        PolicyApplicationTrace policy = PolicyApplicationTrace.of("profile", "CategorySemanticProfile", "C", false);
        RecommendationTrace trace = RecommendationTrace.builder(PromptCategory.WRITING, ActionIntent.GENERATE, true)
                .compatibilityTrace(CompatibilityTrace.of(List.of("G1"), List.of("a1"), policy))
                .expansionTrace(ExpansionTrace.of(List.of("a1", "a2"), policy))
                .actionOrderingTrace(OrderingTrace.of(policy, true, List.of(
                        new OrderingTrace.OrderedItemTrace("a2", "fallback", true),
                        new OrderingTrace.OrderedItemTrace("a1", "preference", false)
                )))
                .actionTraces(List.of(
                        ActionRecommendationTrace.of("a2", "stage", policy),
                        ActionRecommendationTrace.of("a1", "stage", policy)
                ))
                .roleTraces(List.of(RoleRecommendationTrace.of("r1", "stage", policy)))
                .build();

        RecommendationTraceSnapshot snapshot = RecommendationTraceSnapshot.from(trace);

        assertThat(snapshot.categoryKey()).isEqualTo(PromptCategory.WRITING.name());
        assertThat(snapshot.intentKey()).isEqualTo(ActionIntent.GENERATE.name());
        assertThat(snapshot.fallbackIntentUsed()).isTrue();
        assertThat(snapshot.compatibilitySourceIds()).containsExactly("CategorySemanticProfile");
        assertThat(snapshot.expansionSourceIds()).containsExactly("CategorySemanticProfile");
        assertThat(snapshot.orderingFallbackApplied()).isTrue();
        assertThat(snapshot.actionKeysInOrder()).containsExactly("a2", "a1");
        assertThat(snapshot.roleKeysInOrder()).containsExactly("r1");
    }

    @Test
    void auditRecord_of_containsInputResultAndTraceSummary() {
        PolicyApplicationTrace policy = PolicyApplicationTrace.of("profile", "P", "C", false);
        RecommendationTrace trace = RecommendationTrace.builder(PromptCategory.WRITING, ActionIntent.GENERATE, false)
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

        RecommendationAuditRecord record = RecommendationAuditRecord.of(
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                "article_writing",
                "content_writer",
                List.of("article_writing", "essay_writing"),
                List.of("content_writer"),
                trace
        );

        assertThat(record.timestamp()).isNotNull();
        assertThat(record.categoryKey()).isEqualTo(PromptCategory.WRITING.name());
        assertThat(record.intentKey()).isEqualTo(ActionIntent.GENERATE.name());
        assertThat(record.selectedActionKey()).isEqualTo("article_writing");
        assertThat(record.selectedRoleKey()).isEqualTo("content_writer");
        assertThat(record.recommendedActionKeys()).containsExactly("article_writing", "essay_writing");
        assertThat(record.recommendedRoleKeys()).containsExactly("content_writer");
        assertThat(record.traceSummary()).isNotNull();
        assertThat(record.traceSummary().actionKeysInOrder()).isNotEmpty();
    }
}
