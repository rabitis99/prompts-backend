package org.example.sharedprompts.domain.prompt.adapter.in.web.assembler;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptRecommendationResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendedActionResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendedRoleResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.application.semantic.explanation.RecommendationExplanationAssembler;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.category.writing.WritingRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assembler: internal RecommendPromptResult → external PromptRecommendationResponse.
 * Verifies stable keys, nested action→role structure, and no enum/internal names as contract.
 */
class DefaultRecommendationResponseAssemblerTest {

    private final RecommendationExplanationAssembler explanationAssembler = new DefaultRecommendationExplanationAssembler();
    private final DefaultRecommendationResponseAssembler assembler = new DefaultRecommendationResponseAssembler(explanationAssembler);

    @Test
    void toResponse_producesCategoryIntentActionRoleStructure() {
        RecommendPromptResult result = new RecommendPromptResult(
                RequestMode.ADVANCED,
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                List.of(ActionIntent.GENERATE, ActionIntent.REWRITE),
                WritingRoleType.CONTENT_WRITER,
                List.of(WritingRoleType.CONTENT_WRITER, WritingRoleType.COPYWRITER),
                WritingActionType.ARTICLE_WRITING,
                List.of(WritingActionType.ARTICLE_WRITING, WritingActionType.ESSAY_WRITING),
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                Map.of("intent", "user"),
                List.of("Hint A"),
                List.of("Warning 1"),
                List.of(),
                ActionIntent.GENERATE.name()
        );

        PromptRecommendationResponse response = assembler.toResponse(result);

        assertThat(response.category()).isNotNull();
        assertThat(response.category().key()).isEqualTo(PromptCategory.WRITING.key());
        assertThat(response.intent()).isNotNull();
        assertThat(response.intent().key()).isEqualTo(ActionIntent.GENERATE.name());
        assertThat(response.recommendedActions()).hasSize(2);
        assertThat(response.metadata()).isNotNull();
        assertThat(response.metadata().hints()).containsExactly("Hint A");
        assertThat(response.metadata().warnings()).containsExactly("Warning 1");
    }

    @Test
    void toResponse_actionAndRoleUseStableKeysNotEnumName() {
        RecommendPromptResult result = new RecommendPromptResult(
                RequestMode.SIMPLE,
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                List.of(),
                WritingRoleType.TECHNICAL_WRITER,
                List.of(WritingRoleType.TECHNICAL_WRITER),
                WritingActionType.TECHNICAL_WRITING,
                List.of(WritingActionType.TECHNICAL_WRITING),
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        PromptRecommendationResponse response = assembler.toResponse(result);

        assertThat(response.recommendedActions()).hasSize(1);
        RecommendedActionResponse action = response.recommendedActions().get(0);
        assertThat(action.actionKey()).isEqualTo(WritingActionType.TECHNICAL_WRITING.key());
        assertThat(action.actionKey()).isNotEqualTo(WritingActionType.TECHNICAL_WRITING.name());
        assertThat(action.actionDisplayName()).isEqualTo(WritingActionType.TECHNICAL_WRITING.getDisplayNameKo());

        assertThat(action.recommendedRoles()).hasSize(1);
        RecommendedRoleResponse role = action.recommendedRoles().get(0);
        assertThat(role.roleKey()).isEqualTo(WritingRoleType.TECHNICAL_WRITER.key());
        assertThat(role.roleKey()).isNotEqualTo(WritingRoleType.TECHNICAL_WRITER.name());
    }

    @Test
    void toResponse_eachActionHasNestedRecommendedRoles() {
        RecommendPromptResult result = new RecommendPromptResult(
                RequestMode.ADVANCED,
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                List.of(),
                null,
                List.of(WritingRoleType.CONTENT_WRITER, WritingRoleType.COPYWRITER),
                null,
                List.of(WritingActionType.ARTICLE_WRITING, WritingActionType.ESSAY_WRITING),
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        PromptRecommendationResponse response = assembler.toResponse(result);

        assertThat(response.recommendedActions()).hasSize(2);
        for (RecommendedActionResponse ar : response.recommendedActions()) {
            assertThat(ar.recommendedRoles())
                    .hasSize(2)
                    .extracting(RecommendedRoleResponse::roleKey)
                    .containsExactly(WritingRoleType.CONTENT_WRITER.key(), WritingRoleType.COPYWRITER.key());
        }
    }

    @Test
    void toResponse_nullResult_returnsEmptySafeResponse() {
        PromptRecommendationResponse response = assembler.toResponse(null);

        assertThat(response.category()).isNull();
        assertThat(response.intent()).isNull();
        assertThat(response.recommendedActions()).isEmpty();
        assertThat(response.metadata()).isNotNull();
        assertThat(response.metadata().hints()).isEmpty();
    }

    @Test
    void toResponse_preservesActionOrderFromService() {
        List<WritingActionType> order = List.of(
                WritingActionType.ESSAY_WRITING,
                WritingActionType.ARTICLE_WRITING,
                WritingActionType.TECHNICAL_WRITING
        );
        RecommendPromptResult result = new RecommendPromptResult(
                RequestMode.ADVANCED,
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                List.of(),
                null,
                List.of(WritingRoleType.CONTENT_WRITER),
                null,
                List.copyOf(order),
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        PromptRecommendationResponse response = assembler.toResponse(result);

        assertThat(response.recommendedActions())
                .extracting(RecommendedActionResponse::actionKey)
                .containsExactly(
                        WritingActionType.ESSAY_WRITING.key(),
                        WritingActionType.ARTICLE_WRITING.key(),
                        WritingActionType.TECHNICAL_WRITING.key()
                );
    }

    @Test
    void toResponse_emptyCandidates_producesEmptyActionsAndRoles() {
        RecommendPromptResult result = new RecommendPromptResult(
                RequestMode.SIMPLE,
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        PromptRecommendationResponse response = assembler.toResponse(result);

        assertThat(response.recommendedActions()).isEmpty();
        assertThat(response.category().key()).isEqualTo(PromptCategory.WRITING.key());
        assertThat(response.intent().key()).isEqualTo(ActionIntent.GENERATE.name());
    }

    @Test
    void toResponse_includeExplanationTrue_fillsActionAndRoleReasonWhenTracePresent() {
        org.example.sharedprompts.domain.prompt.domain.semantic.trace.PolicyApplicationTrace policy =
                org.example.sharedprompts.domain.prompt.domain.semantic.trace.PolicyApplicationTrace.of("profile", "P", "C", false);
        org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace trace =
                org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace.builder(
                        PromptCategory.WRITING, ActionIntent.GENERATE, false)
                .actionTraces(List.of(
                        org.example.sharedprompts.domain.prompt.domain.semantic.trace.ActionRecommendationTrace.of(
                                WritingActionType.ARTICLE_WRITING.key(), "compatibility+expansion", policy),
                        org.example.sharedprompts.domain.prompt.domain.semantic.trace.ActionRecommendationTrace.of(
                                WritingActionType.ESSAY_WRITING.key(), "compatibility+expansion", policy)
                ))
                .roleTraces(List.of(
                        org.example.sharedprompts.domain.prompt.domain.semantic.trace.RoleRecommendationTrace.of(
                                WritingRoleType.CONTENT_WRITER.key(), "profile", policy)
                ))
                .build();

        RecommendPromptResult result = new RecommendPromptResult(
                RequestMode.ADVANCED,
                PromptCategory.WRITING,
                ActionIntent.GENERATE,
                List.of(),
                null,
                List.of(WritingRoleType.CONTENT_WRITER),
                null,
                List.of(WritingActionType.ARTICLE_WRITING, WritingActionType.ESSAY_WRITING),
                ToneType.NEUTRAL,
                StyleType.NARRATIVE,
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                java.util.Optional.of(trace)
        );

        PromptRecommendationResponse withExplanation = assembler.toResponse(result, true);
        PromptRecommendationResponse withoutExplanation = assembler.toResponse(result, false);

        assertThat(withExplanation.recommendedActions()).hasSize(2);
        assertThat(withExplanation.recommendedActions().get(0).reason()).isNotBlank();
        assertThat(withExplanation.recommendedActions().get(0).source()).isNotNull();
        assertThat(withExplanation.recommendedActions().get(0).recommendedRoles().get(0).reason()).isNotBlank();

        assertThat(withoutExplanation.recommendedActions().get(0).reason()).isNull();
        assertThat(withoutExplanation.recommendedActions().get(0).source()).isNull();
    }
}
