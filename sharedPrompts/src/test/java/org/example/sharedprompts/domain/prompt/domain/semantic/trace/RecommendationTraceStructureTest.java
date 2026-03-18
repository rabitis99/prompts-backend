package org.example.sharedprompts.domain.prompt.domain.semantic.trace;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies internal trace is structurally created and phase-wise traces are separated.
 */
class RecommendationTraceStructureTest {

    @Test
    void traceBuilder_producesStructuredTraceWithAllPhases() {
        PolicyApplicationTrace compatPolicy = PolicyApplicationTrace.of("profile", "CategorySemanticProfile", "Compatibility", false);
        CompatibilityTrace compatTrace = CompatibilityTrace.of(
                List.of("LONG_FORM_WRITING"),
                List.of("article_writing", "essay_writing"),
                compatPolicy
        );
        PolicyApplicationTrace expansionPolicy = PolicyApplicationTrace.of("in-memory", "DefaultRecommendationConcreteActionExpander", "Expansion", false);
        ExpansionTrace expansionTrace = ExpansionTrace.of(List.of("article_writing", "essay_writing"), expansionPolicy);
        List<OrderingTrace.OrderedItemTrace> items = List.of(
                new OrderingTrace.OrderedItemTrace("article_writing", "preference-source", false),
                new OrderingTrace.OrderedItemTrace("essay_writing", "fallback-stable-key", true)
        );
        PolicyApplicationTrace orderPolicy = PolicyApplicationTrace.of("in-memory", "DefaultActionRecommendationPreferenceSource", "ActionRecommendationOrder", false);
        OrderingTrace orderingTrace = OrderingTrace.of(orderPolicy, true, items);

        RecommendationTrace trace = RecommendationTrace.builder(PromptCategory.WRITING, ActionIntent.GENERATE, false)
                .compatibilityTrace(compatTrace)
                .expansionTrace(expansionTrace)
                .actionOrderingTrace(orderingTrace)
                .actionTraces(List.of(
                        ActionRecommendationTrace.of("article_writing", "compatibility+expansion", compatPolicy),
                        ActionRecommendationTrace.of("essay_writing", "compatibility+expansion", compatPolicy)
                ))
                .roleTraces(List.of(
                        RoleRecommendationTrace.of("content_writer", "profile-recommended-roles", compatPolicy)
                ))
                .build();

        assertThat(trace.category()).isEqualTo(PromptCategory.WRITING);
        assertThat(trace.intent()).isEqualTo(ActionIntent.GENERATE);
        assertThat(trace.fallbackIntentUsed()).isFalse();
        assertThat(trace.compatibilityTrace()).isPresent();
        assertThat(trace.compatibilityTrace().get().allowedGroupNames()).containsExactly("LONG_FORM_WRITING");
        assertThat(trace.compatibilityTrace().get().allowedActionKeys()).containsExactly("article_writing", "essay_writing");
        assertThat(trace.expansionTrace()).isPresent();
        assertThat(trace.expansionTrace().get().expandedActionKeys()).containsExactly("article_writing", "essay_writing");
        assertThat(trace.actionOrderingTrace()).isPresent();
        assertThat(trace.actionOrderingTrace().get().fallbackOrderingApplied()).isTrue();
        assertThat(trace.actionOrderingTrace().get().orderedItems()).hasSize(2);
        assertThat(trace.actionTraces()).hasSize(2);
        assertThat(trace.roleTraces()).hasSize(1);
        assertThat(trace.roleTraces().get(0).roleKey()).isEqualTo("content_writer");
    }

    @Test
    void compatibility_expansion_ordering_traces_areSeparate() {
        RecommendationTrace trace = RecommendationTrace.builder(PromptCategory.WRITING, ActionIntent.REWRITE, true)
                .compatibilityTrace(CompatibilityTrace.of(List.of("A"), List.of("a1"), PolicyApplicationTrace.of("p", "P", "C", false)))
                .expansionTrace(ExpansionTrace.of(List.of("a1", "a2"), PolicyApplicationTrace.of("m", "E", "E", false)))
                .actionOrderingTrace(OrderingTrace.of(
                        PolicyApplicationTrace.of("m", "O", "O", true),
                        true,
                        List.of(new OrderingTrace.OrderedItemTrace("a2", "fallback", true), new OrderingTrace.OrderedItemTrace("a1", "fallback", true))
                ))
                .build();

        assertThat(trace.compatibilityTrace()).isPresent();
        assertThat(trace.compatibilityTrace().get().policyTrace().sourceId()).isEqualTo("P");
        assertThat(trace.expansionTrace()).isPresent();
        assertThat(trace.expansionTrace().get().policyTrace().sourceId()).isEqualTo("E");
        assertThat(trace.actionOrderingTrace()).isPresent();
        assertThat(trace.actionOrderingTrace().get().policyTrace().sourceId()).isEqualTo("O");
        assertThat(trace.actionOrderingTrace().get().fallbackOrderingApplied()).isTrue();
    }
}
