package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace;

import java.util.List;
import java.util.Optional;

/**
 * Result of category-aware recommendation.
 * Distinguishes user-provided vs recommended values; no silent invention.
 * Carries optional structured trace for explainability/audit; hints are built from trace in assembler layer.
 */
public record RecommendationResult(
        PromptCategory category,
        ActionIntent intent,
        Optional<RoleTypeInterface> recommendedRole,
        Optional<ActionTypeInterface> recommendedAction,
        List<RoleTypeInterface> roleCandidates,
        List<ActionTypeInterface> actionCandidates,
        List<String> recommendationHints,
        Optional<RecommendationTrace> trace
) {
    public RecommendationResult {
        roleCandidates = roleCandidates != null ? List.copyOf(roleCandidates) : List.of();
        actionCandidates = actionCandidates != null ? List.copyOf(actionCandidates) : List.of();
        recommendationHints = recommendationHints != null ? List.copyOf(recommendationHints) : List.of();
        trace = trace != null ? trace : Optional.empty();
    }

    /** Backward-compat: trace is empty. */
    public RecommendationResult(
            PromptCategory category,
            ActionIntent intent,
            Optional<RoleTypeInterface> recommendedRole,
            Optional<ActionTypeInterface> recommendedAction,
            List<RoleTypeInterface> roleCandidates,
            List<ActionTypeInterface> actionCandidates,
            List<String> recommendationHints
    ) {
        this(category, intent, recommendedRole, recommendedAction, roleCandidates, actionCandidates, recommendationHints, Optional.empty());
    }
}
