package org.example.sharedprompts.domain.prompt.adapter.in.web.assembler;

import org.example.sharedprompts.domain.prompt.application.semantic.explanation.RecommendationExplanationAssembler;
import org.example.sharedprompts.domain.prompt.application.semantic.explanation.RecommendationExplanationView;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Converts internal trace to user-facing hints and explanation view.
 * Implements application port; response assembler maps view to DTO.
 */
@Component
public class DefaultRecommendationExplanationAssembler implements RecommendationExplanationAssembler {

    @Override
    public List<String> toHints(Optional<RecommendationTrace> trace) {
        if (trace.isEmpty()) return List.of();
        List<String> hints = new ArrayList<>();
        RecommendationTrace t = trace.get();
        if (t.fallbackIntentUsed()) {
            hints.add("Intent was omitted or adjusted; profile fallback was applied.");
        }
        t.compatibilityTrace().ifPresent(ct ->
                hints.add("Actions and roles were chosen to match this category and intent.")
        );
        t.actionOrderingTrace().ifPresent(ot -> {
            if (ot.fallbackOrderingApplied()) {
                hints.add("Order was determined by default ordering when no preference was set.");
            } else {
                hints.add("Order follows your preference policy for this category and intent.");
            }
        });
        return hints;
    }

    @Override
    public RecommendationExplanationView toExplanation(Optional<RecommendationTrace> trace) {
        if (trace.isEmpty()) {
            return new RecommendationExplanationView(List.of(), List.of(), List.of(), false, null);
        }
        RecommendationTrace t = trace.get();
        List<String> summaryHints = toHints(trace);
        List<RecommendationExplanationView.ActionItem> actionExplanations = t.actionTraces().stream()
                .map(a -> new RecommendationExplanationView.ActionItem(
                        a.actionKey(),
                        toActionReason(t, a.actionKey()),
                        a.inclusionPolicy().sourceName(),
                        a.orderingTrace().map(o -> o.fallbackOrderingApplied() ? "default order" : "preference").orElse(null)
                ))
                .collect(Collectors.toList());
        List<RecommendationExplanationView.RoleItem> roleExplanations = t.roleTraces().stream()
                .map(r -> new RecommendationExplanationView.RoleItem(
                        r.roleKey(),
                        toRoleReason(t, r.roleKey()),
                        r.inclusionPolicy().sourceName(),
                        r.orderingTrace().map(o -> o.fallbackOrderingApplied() ? "default order" : "preference").orElse(null)
                ))
                .collect(Collectors.toList());
        String fallbackReason = t.fallbackIntentUsed() ? "Profile fallback was applied for intent." : null;
        return new RecommendationExplanationView(
                summaryHints,
                actionExplanations,
                roleExplanations,
                t.fallbackIntentUsed(),
                fallbackReason
        );
    }

    public String toActionReason(RecommendationTrace trace, String actionKey) {
        return trace.actionTraces().stream()
                .filter(a -> actionKey.equals(a.actionKey()))
                .findFirst()
                .map(a -> {
                    if (a.inclusionStage() != null && !a.inclusionStage().isEmpty()) {
                        return "Recommended for this intent; " + a.inclusionStage() + ".";
                    }
                    return "This action fits the selected category and intent.";
                })
                .orElse("This action fits the selected category and intent.");
    }

    public String toRoleReason(RecommendationTrace trace, String roleKey) {
        return trace.roleTraces().stream()
                .filter(r -> roleKey.equals(r.roleKey()))
                .findFirst()
                .map(r -> {
                    if (r.inclusionStage() != null && !r.inclusionStage().isEmpty()) {
                        return "Recommended for this intent; " + r.inclusionStage() + ".";
                    }
                    return "This role fits the selected category and intent.";
                })
                .orElse("This role fits the selected category and intent.");
    }
}
