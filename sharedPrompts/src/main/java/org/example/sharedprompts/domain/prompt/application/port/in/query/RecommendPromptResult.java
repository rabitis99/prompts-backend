package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 추천 플로우 결과. Optional trace for explainability/audit; hints are built from trace in resolver. */
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
        String defaultSelection,
        Optional<RecommendationTrace> trace
) {
    public RecommendPromptResult {
        intentCandidates = intentCandidates != null ? List.copyOf(intentCandidates) : List.of();
        roleCandidates = roleCandidates != null ? List.copyOf(roleCandidates) : List.of();
        actionCandidates = actionCandidates != null ? List.copyOf(actionCandidates) : List.of();
        recommendationHints = recommendationHints != null ? List.copyOf(recommendationHints) : List.of();
        validationWarnings = validationWarnings != null ? List.copyOf(validationWarnings) : List.of();
        fallbackApplied = fallbackApplied != null ? List.copyOf(fallbackApplied) : List.of();
        axisSources = axisSources != null ? Map.copyOf(axisSources) : null;
        trace = trace != null ? trace : Optional.empty();
    }

    /** Backward-compat: trace empty. */
    public RecommendPromptResult(
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
            String defaultSelection
    ) {
        this(requestMode, category, recommendedIntent, intentCandidates, recommendedRole, roleCandidates,
                recommendedAction, actionCandidates, recommendedTone, recommendedStyle, axisSources,
                recommendationHints, validationWarnings, fallbackApplied, defaultSelection, Optional.empty());
    }
}
