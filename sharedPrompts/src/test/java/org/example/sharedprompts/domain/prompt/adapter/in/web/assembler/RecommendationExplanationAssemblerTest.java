package org.example.sharedprompts.domain.prompt.adapter.in.web.assembler;

import org.example.sharedprompts.domain.prompt.application.semantic.explanation.RecommendationExplanationView;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * External explanation assembler: internal trace → summary hints and per-item explanation.
 */
class RecommendationExplanationAssemblerTest {

    private final DefaultRecommendationExplanationAssembler assembler = new DefaultRecommendationExplanationAssembler();

    @Test
    void toHints_emptyTrace_returnsEmpty() {
        List<String> hints = assembler.toHints(Optional.empty());
        assertThat(hints).isEmpty();
    }

    @Test
    void toHints_withTrace_returnsUserFriendlySummary() {
        PolicyApplicationTrace policy = PolicyApplicationTrace.of("profile", "CategorySemanticProfile", "Compatibility", false);
        RecommendationTrace trace = RecommendationTrace.builder(
                org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory.WRITING,
                org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent.GENERATE,
                true
        )
                .compatibilityTrace(CompatibilityTrace.of(List.of(), List.of(), policy))
                .actionOrderingTrace(OrderingTrace.of(policy, true, List.of()))
                .build();

        List<String> hints = assembler.toHints(Optional.of(trace));

        assertThat(hints).isNotEmpty();
        assertThat(hints.stream().anyMatch(h -> h.toLowerCase().contains("fallback"))).isTrue();
    }

    @Test
    void toExplanation_convertsTraceToSummaryAndPerItemExplanation() {
        PolicyApplicationTrace policy = PolicyApplicationTrace.of("profile", "P", "C", false);
        RecommendationTrace trace = RecommendationTrace.builder(
                org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory.WRITING,
                org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent.GENERATE,
                false
        )
                .compatibilityTrace(CompatibilityTrace.of(List.of(), List.of(), policy))
                .actionTraces(List.of(
                        ActionRecommendationTrace.of("article_writing", "compatibility+expansion", policy),
                        ActionRecommendationTrace.of("essay_writing", "compatibility+expansion", policy)
                ))
                .roleTraces(List.of(RoleRecommendationTrace.of("content_writer", "profile-recommended-roles", policy)))
                .build();

        RecommendationExplanationView view = assembler.toExplanation(Optional.of(trace));

        assertThat(view.summaryHints()).isNotEmpty();
        assertThat(view.actionExplanations()).hasSize(2);
        assertThat(view.roleExplanations()).hasSize(1);
        assertThat(view.actionExplanations().stream().map(RecommendationExplanationView.ActionItem::actionKey))
                .containsExactlyInAnyOrder("article_writing", "essay_writing");
        assertThat(view.roleExplanations().get(0).roleKey()).isEqualTo("content_writer");
        assertThat(view.actionExplanations().get(0).reason()).isNotBlank();
        assertThat(view.roleExplanations().get(0).reason()).isNotBlank();
    }

    @Test
    void toActionReason_and_toRoleReason_returnShortReasons() {
        PolicyApplicationTrace policy = PolicyApplicationTrace.of("profile", "P", "C", false);
        RecommendationTrace trace = RecommendationTrace.builder(
                org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory.WRITING,
                org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent.GENERATE,
                false
        )
                .actionTraces(List.of(ActionRecommendationTrace.of("article_writing", "compatibility+expansion", policy)))
                .roleTraces(List.of(RoleRecommendationTrace.of("content_writer", "profile", policy)))
                .build();

        String actionReason = assembler.toActionReason(trace, "article_writing");
        String roleReason = assembler.toRoleReason(trace, "content_writer");

        assertThat(actionReason).isNotBlank();
        assertThat(roleReason).isNotBlank();
    }
}
