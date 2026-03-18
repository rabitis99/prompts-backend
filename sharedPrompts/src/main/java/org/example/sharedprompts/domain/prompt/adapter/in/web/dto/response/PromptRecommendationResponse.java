package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * UX-aligned recommendation API response: Category → Intent → Action → Role.
 * <p>External contract: categoryKey, intentKey, actionKey, roleKey are stable identifiers.
 * Display names are convenience only.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record PromptRecommendationResponse(

        /** Category block */
        @JsonProperty("category")
        CategoryIntentBlock category,

        /** Intent block */
        @JsonProperty("intent")
        CategoryIntentBlock intent,

        /** Recommended actions, each with nested recommended roles */
        @JsonProperty("recommended_actions")
        List<RecommendedActionResponse> recommendedActions,

        /** Optional: hints, warnings, explainability metadata */
        @JsonProperty("metadata")
        RecommendationMetadata metadata
) {
    public PromptRecommendationResponse {
        recommendedActions = recommendedActions != null ? List.copyOf(recommendedActions) : List.of();
    }

    /** Stable key + display name block for category or intent */
    public record CategoryIntentBlock(
            @JsonProperty("key") String key,
            @JsonProperty("display_name") String displayName
    ) {}

    /** Optional hints, warnings, fallback info */
    public record RecommendationMetadata(
            @JsonProperty("hints") List<String> hints,
            @JsonProperty("warnings") List<String> warnings,
            @JsonProperty("fallback_applied") List<String> fallbackApplied,
            @JsonProperty("request_mode") String requestMode
    ) {
        public RecommendationMetadata {
            hints = hints != null ? List.copyOf(hints) : List.of();
            warnings = warnings != null ? List.copyOf(warnings) : List.of();
            fallbackApplied = fallbackApplied != null ? List.copyOf(fallbackApplied) : List.of();
        }
    }
}
