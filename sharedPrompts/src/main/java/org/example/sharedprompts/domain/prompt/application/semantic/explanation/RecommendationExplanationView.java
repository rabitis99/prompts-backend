package org.example.sharedprompts.domain.prompt.application.semantic.explanation;

import java.util.List;

/**
 * Application-level explanation data (no adapter DTO dependency).
 * Adapter maps this to RecommendationExplanation DTO for response.
 */
public record RecommendationExplanationView(
        List<String> summaryHints,
        List<ActionItem> actionExplanations,
        List<RoleItem> roleExplanations,
        boolean fallbackIntentUsed,
        String fallbackReason
) {
    public RecommendationExplanationView {
        summaryHints = summaryHints != null ? List.copyOf(summaryHints) : List.of();
        actionExplanations = actionExplanations != null ? List.copyOf(actionExplanations) : List.of();
        roleExplanations = roleExplanations != null ? List.copyOf(roleExplanations) : List.of();
    }

    public record ActionItem(String actionKey, String reason, String source, String orderingBasis) {}
    public record RoleItem(String roleKey, String reason, String source, String orderingBasis) {}
}
