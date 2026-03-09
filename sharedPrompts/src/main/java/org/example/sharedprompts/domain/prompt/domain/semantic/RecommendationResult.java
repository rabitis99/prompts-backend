package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;
import java.util.Optional;

/**
 * Result of category-aware recommendation.
 * Distinguishes user-provided vs recommended values; no silent invention.
 */
public record RecommendationResult(
        PromptCategory category,
        ActionIntent intent,
        Optional<RoleTypeInterface> recommendedRole,
        Optional<ActionTypeInterface> recommendedAction,
        List<RoleTypeInterface> roleCandidates,
        List<ActionTypeInterface> actionCandidates,
        List<String> recommendationHints
) {
    public RecommendationResult {
        roleCandidates = roleCandidates != null ? List.copyOf(roleCandidates) : List.of();
        actionCandidates = actionCandidates != null ? List.copyOf(actionCandidates) : List.of();
        recommendationHints = recommendationHints != null ? List.copyOf(recommendationHints) : List.of();
    }
}
