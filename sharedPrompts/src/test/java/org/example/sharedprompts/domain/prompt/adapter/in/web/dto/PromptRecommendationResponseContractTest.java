package org.example.sharedprompts.domain.prompt.adapter.in.web.dto;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptRecommendationResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendedActionResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendedRoleResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test: response DTO shape and stable key usage.
 * Protects Category → Intent → Action → Role structure and key-as-contract.
 */
class PromptRecommendationResponseContractTest {

    @Test
    void responseShape_hasCategoryIntentActionsWithNestedRoles() {
        PromptRecommendationResponse response = new PromptRecommendationResponse(
                new PromptRecommendationResponse.CategoryIntentBlock("PROMPT_CATEGORY.WRITING", "글쓰기"),
                new PromptRecommendationResponse.CategoryIntentBlock("GENERATE", "Generate"),
                List.of(
                        RecommendedActionResponse.of(
                                "ACTION.WRITING.ARTICLE_WRITING",
                                "기사 작성",
                                "LONG_FORM_WRITING",
                                List.of(
                                        RecommendedRoleResponse.of("ROLE.WRITING.CONTENT_WRITER", "콘텐츠 작가"),
                                        RecommendedRoleResponse.of("ROLE.WRITING.COPYWRITER", "카피라이터")
                                )
                        )
                ),
                new PromptRecommendationResponse.RecommendationMetadata(List.of("hint"), List.of(), List.of(), "ADVANCED")
        );

        assertThat(response.category()).isNotNull();
        assertThat(response.category().key()).isEqualTo("PROMPT_CATEGORY.WRITING");
        assertThat(response.category().displayName()).isEqualTo("글쓰기");

        assertThat(response.intent()).isNotNull();
        assertThat(response.intent().key()).isEqualTo("GENERATE");

        assertThat(response.recommendedActions()).hasSize(1);
        RecommendedActionResponse action = response.recommendedActions().get(0);
        assertThat(action.actionKey()).isEqualTo("ACTION.WRITING.ARTICLE_WRITING");
        assertThat(action.actionDisplayName()).isEqualTo("기사 작성");
        assertThat(action.recommendedRoles()).hasSize(2);
        assertThat(action.recommendedRoles().get(0).roleKey()).isEqualTo("ROLE.WRITING.CONTENT_WRITER");
        assertThat(action.recommendedRoles().get(1).roleKey()).isEqualTo("ROLE.WRITING.COPYWRITER");
    }

    @Test
    void keysAreStableIdentifiers_displayNamesAreAuxiliary() {
        RecommendedRoleResponse role = RecommendedRoleResponse.of("ROLE.WRITING.TECHNICAL_WRITER", "기술 문서 작성자");
        assertThat(role.roleKey()).isEqualTo("ROLE.WRITING.TECHNICAL_WRITER");
        assertThat(role.roleDisplayName()).isEqualTo("기술 문서 작성자");
        assertThat(role.roleKey()).isNotEqualTo(role.roleDisplayName());
    }

    @Test
    void optionalExplainabilityFields_canBeNull() {
        RecommendedRoleResponse withReason = RecommendedRoleResponse.withExplanation(
                "ROLE.WRITING.COPYWRITER", "카피라이터", "preferred for intent", "policy", null
        );
        assertThat(withReason.reason()).isEqualTo("preferred for intent");
        assertThat(withReason.source()).isEqualTo("policy");

        RecommendedRoleResponse minimal = RecommendedRoleResponse.of("ROLE.WRITING.CONTENT_WRITER", "콘텐츠 작가");
        assertThat(minimal.reason()).isNull();
        assertThat(minimal.source()).isNull();
    }

    @Test
    void newActionOrRoleAdded_doesNotRequireDtoStructureChange() {
        RecommendedActionResponse action = RecommendedActionResponse.of(
                "ACTION.WRITING.NEW_ACTION",
                "새 작업",
                "LONG_FORM_WRITING",
                List.of(
                        RecommendedRoleResponse.of("ROLE.WRITING.NEW_ROLE", "새 역할")
                )
        );
        assertThat(action.actionKey()).isEqualTo("ACTION.WRITING.NEW_ACTION");
        assertThat(action.recommendedRoles()).hasSize(1);
        assertThat(action.recommendedRoles().get(0).roleKey()).isEqualTo("ROLE.WRITING.NEW_ROLE");
    }

    @Test
    void explanationMetadata_whenPresent_includedInResponse() {
        PromptRecommendationResponse.RecommendationMetadata withMeta = new PromptRecommendationResponse.RecommendationMetadata(
                List.of("hint1"),
                List.of("warning1"),
                List.of("fallback: intent"),
                "ADVANCED"
        );
        assertThat(withMeta.hints()).containsExactly("hint1");
        assertThat(withMeta.warnings()).containsExactly("warning1");
        assertThat(withMeta.fallbackApplied()).containsExactly("fallback: intent");
        assertThat(withMeta.requestMode()).isEqualTo("ADVANCED");
    }

    @Test
    void explanationMetadata_whenAbsent_optionalEmpty() {
        PromptRecommendationResponse.RecommendationMetadata empty = new PromptRecommendationResponse.RecommendationMetadata(
                List.of(), List.of(), List.of(), null
        );
        assertThat(empty.hints()).isEmpty();
        assertThat(empty.warnings()).isEmpty();
        assertThat(empty.fallbackApplied()).isEmpty();
        assertThat(empty.requestMode()).isNull();
    }
}
