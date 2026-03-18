package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import java.util.List;

/**
 * External explanation model: user/API-friendly summary of why recommendations were made.
 * Not the full internal trace; use for optional explanation in response.
 */
public record RecommendationExplanation(
        List<String> summaryHints,
        List<ActionExplanationItem> actionExplanations,
        List<RoleExplanationItem> roleExplanations,
        boolean fallbackIntentUsed,
        String fallbackIntentReason
) {
    public RecommendationExplanation {
        summaryHints = summaryHints != null ? List.copyOf(summaryHints) : List.of();
        actionExplanations = actionExplanations != null ? List.copyOf(actionExplanations) : List.of();
        roleExplanations = roleExplanations != null ? List.copyOf(roleExplanations) : List.of();
    }

    public record ActionExplanationItem(String actionKey, String reason, String source, String orderingBasis) {}
    public record RoleExplanationItem(String roleKey, String reason, String source, String orderingBasis) {}
}
