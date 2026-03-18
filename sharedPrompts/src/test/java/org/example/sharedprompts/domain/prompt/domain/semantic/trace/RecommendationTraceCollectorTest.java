package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies trace collector records phases and builds RecommendationTrace.
 */
class RecommendationTraceCollectorTest {

    @Test
    void collector_recordsCompatibilityExpansionOrderingAndBuildsTrace() {
        DefaultRecommendationTraceCollector collector = new DefaultRecommendationTraceCollector(
                PromptCategory.WRITING, ActionIntent.GENERATE, false
        );

        PolicyApplicationTrace compat = PolicyApplicationTrace.of("profile", "CategorySemanticProfile", "Compatibility", false);
        collector.recordCompatibility(List.of("LONG_FORM_WRITING"), List.of("article_writing"), compat);
        collector.recordExpansion(List.of("article_writing", "essay_writing"), compat);
        List<OrderingTrace.OrderedItemTrace> items = List.of(
                new OrderingTrace.OrderedItemTrace("essay_writing", "fallback-stable-key", true),
                new OrderingTrace.OrderedItemTrace("article_writing", "preference-source", false)
        );
        collector.recordActionOrdering(OrderingTrace.of(compat, true, items));
        collector.setActionTraces(List.of(
                ActionRecommendationTrace.of("essay_writing", "compatibility+expansion", compat),
                ActionRecommendationTrace.of("article_writing", "compatibility+expansion", compat)
        ));
        collector.setRoleTraces(List.of(RoleRecommendationTrace.of("content_writer", "profile", compat)));

        RecommendationTrace trace = collector.build();

        assertThat(trace.category()).isEqualTo(PromptCategory.WRITING);
        assertThat(trace.intent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(trace.compatibilityTrace()).isPresent();
        assertThat(trace.expansionTrace()).isPresent();
        assertThat(trace.actionOrderingTrace()).isPresent();
        assertThat(trace.actionOrderingTrace().get().fallbackOrderingApplied()).isTrue();
        assertThat(trace.actionOrderingTrace().get().orderedItems())
                .extracting(OrderingTrace.OrderedItemTrace::itemKey)
                .containsExactly("essay_writing", "article_writing");
        assertThat(trace.actionTraces()).hasSize(2);
        assertThat(trace.roleTraces()).hasSize(1);
    }

    @Test
    void fallbackOrderingApplied_recordedInTrace() {
        DefaultRecommendationTraceCollector collector = new DefaultRecommendationTraceCollector(
                PromptCategory.WRITING, ActionIntent.GENERATE, false
        );
        PolicyApplicationTrace policy = PolicyApplicationTrace.of("in-memory", "DefaultActionRecommendationPreferenceSource", "Order", true);
        collector.recordActionOrdering(OrderingTrace.of(policy, true, List.of(
                new OrderingTrace.OrderedItemTrace("x", "fallback-stable-key", true)
        )));
        RecommendationTrace trace = collector.build();

        assertThat(trace.actionOrderingTrace()).isPresent();
        assertThat(trace.actionOrderingTrace().get().fallbackOrderingApplied()).isTrue();
        assertThat(trace.actionOrderingTrace().get().orderedItems().get(0).fromFallback()).isTrue();
    }
}
