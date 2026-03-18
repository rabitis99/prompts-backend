package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Action recommendation item in API response, with nested recommended roles.
 * <p>Contract: {@code actionKey} is the stable identifier; {@code actionDisplayName} is convenience only.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RecommendedActionResponse(

        /** Stable identifier for API/UI/storage. External contract. */
        @JsonProperty("action_key")
        String actionKey,

        /** Display label; not part of contract. */
        @JsonProperty("action_display_name")
        String actionDisplayName,

        /** Canonical capability group (e.g. LONG_FORM_WRITING). */
        @JsonProperty("action_group")
        String actionGroup,

        /** Optional: objective or intent alignment. */
        @JsonProperty("objective")
        String objective,

        /** Optional: why this action was recommended (explainability). */
        @JsonProperty("reason")
        String reason,

        /** Optional: source / ordering basis. */
        @JsonProperty("source")
        String source,

        @JsonProperty("ordering_basis")
        String orderingBasis,

        /** Roles recommended for this action (Category → Intent → Action → Role). */
        @JsonProperty("recommended_roles")
        List<RecommendedRoleResponse> recommendedRoles
) {
    public static RecommendedActionResponse of(String actionKey, String actionDisplayName,
                                               String actionGroup, List<RecommendedRoleResponse> recommendedRoles) {
        return new RecommendedActionResponse(
                actionKey, actionDisplayName, actionGroup,
                null, null, null, null,
                recommendedRoles != null ? List.copyOf(recommendedRoles) : List.of()
        );
    }

    public static RecommendedActionResponse withExplanation(String actionKey, String actionDisplayName,
                                                            String actionGroup, String objective,
                                                            String reason, String source, String orderingBasis,
                                                            List<RecommendedRoleResponse> recommendedRoles) {
        return new RecommendedActionResponse(
                actionKey, actionDisplayName, actionGroup,
                objective, reason, source, orderingBasis,
                recommendedRoles != null ? List.copyOf(recommendedRoles) : List.of()
        );
    }
}
