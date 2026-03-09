package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;
import java.util.Map;

/**
 * Result of the recommendation-only flow (POST /prompts/recommend).
 * Mapped to RecommendPromptResponse in the adapter.
 */
public record RecommendPromptResult(
        RequestMode requestMode,
        PromptCategory category,
        ActionIntent recommendedIntent,
        List<ActionIntent> intentCandidates,
        RoleTypeInterface recommendedRole,
        List<RoleTypeInterface> roleCandidates,
        ActionTypeInterface recommendedAction,
        List<ActionTypeInterface> actionCandidates,
        ToneType recommendedTone,
        StyleType recommendedStyle,
        Map<String, String> axisSources,
        List<String> recommendationHints,
        List<String> validationWarnings,
        List<String> fallbackApplied,
        Object defaultSelection
) {
    public RecommendPromptResult {
        intentCandidates = intentCandidates != null ? List.copyOf(intentCandidates) : List.of();
        roleCandidates = roleCandidates != null ? List.copyOf(roleCandidates) : List.of();
        actionCandidates = actionCandidates != null ? List.copyOf(actionCandidates) : List.of();
        recommendationHints = recommendationHints != null ? List.copyOf(recommendationHints) : List.of();
        validationWarnings = validationWarnings != null ? List.copyOf(validationWarnings) : List.of();
        fallbackApplied = fallbackApplied != null ? List.copyOf(fallbackApplied) : List.of();
    }
}
