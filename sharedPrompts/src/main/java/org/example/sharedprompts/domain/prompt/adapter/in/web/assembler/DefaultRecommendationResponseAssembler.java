package org.example.sharedprompts.domain.prompt.adapter.in.web.assembler;

import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.PromptRecommendationResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendationExplanation;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendedActionResponse;
import org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response.RecommendedRoleResponse;
import org.example.sharedprompts.domain.prompt.application.port.in.query.RecommendPromptResult;
import org.example.sharedprompts.domain.prompt.application.semantic.explanation.RecommendationExplanationAssembler;
import org.example.sharedprompts.domain.prompt.application.semantic.explanation.RecommendationExplanationView;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.domain.semantic.trace.RecommendationTrace;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Assembles internal RecommendPromptResult into UX-aligned PromptRecommendationResponse.
 * Uses RecommendationExplanationAssembler (interface) for explanation; maps view to DTO.
 */
@Component
public class DefaultRecommendationResponseAssembler implements RecommendationResponseAssembler {

    private final RecommendationExplanationAssembler explanationAssembler;

    public DefaultRecommendationResponseAssembler(RecommendationExplanationAssembler explanationAssembler) {
        this.explanationAssembler = explanationAssembler;
    }

    @Override
    public PromptRecommendationResponse toResponse(RecommendPromptResult result) {
        return toResponse(result, false);
    }

    @Override
    public PromptRecommendationResponse toResponse(RecommendPromptResult result, boolean includeExplanation) {
        if (result == null) {
            return new PromptRecommendationResponse(
                    null, null, List.of(),
                    new PromptRecommendationResponse.RecommendationMetadata(List.of(), List.of(), List.of(), null)
            );
        }

        PromptCategory category = result.category();
        ActionIntent intent = result.recommendedIntent();

        PromptRecommendationResponse.CategoryIntentBlock categoryBlock = category != null
                ? new PromptRecommendationResponse.CategoryIntentBlock(category.key(), category.getDisplayName())
                : null;

        PromptRecommendationResponse.CategoryIntentBlock intentBlock = intent != null
                ? new PromptRecommendationResponse.CategoryIntentBlock(intent.name(), humanizeIntent(intent.name()))
                : null;

        Optional<RecommendationTrace> trace = result.trace();
        RecommendationExplanationView explanationView = includeExplanation && trace.isPresent()
                ? explanationAssembler.toExplanation(trace)
                : null;
        RecommendationExplanation explanation = explanationView != null ? toExplanationDto(explanationView) : null;

        Map<String, RecommendationExplanation.ActionExplanationItem> actionExplanationByKey = explanation != null
                ? explanation.actionExplanations().stream()
                .collect(Collectors.toMap(RecommendationExplanation.ActionExplanationItem::actionKey, Function.identity()))
                : Map.of();
        Map<String, RecommendationExplanation.RoleExplanationItem> roleExplanationByKey = explanation != null
                ? explanation.roleExplanations().stream()
                .collect(Collectors.toMap(RecommendationExplanation.RoleExplanationItem::roleKey, Function.identity()))
                : Map.of();

        List<RecommendedRoleResponse> roleResponses = toRoleResponses(result.roleCandidates(), roleExplanationByKey);
        List<RecommendedActionResponse> actionResponses = new ArrayList<>();
        for (ActionTypeInterface action : result.actionCandidates()) {
            String actionKey = action.key();
            RecommendationExplanation.ActionExplanationItem ex = actionExplanationByKey.get(actionKey);
            RecommendedActionResponse ar;
            if (ex != null) {
                ar = RecommendedActionResponse.withExplanation(
                        actionKey,
                        action.getDisplayNameKo(),
                        action.getActionGroup() != null ? action.getActionGroup().name() : null,
                        null,
                        ex.reason(),
                        ex.source(),
                        ex.orderingBasis(),
                        roleResponses
                );
            } else {
                ar = RecommendedActionResponse.of(
                        actionKey,
                        action.getDisplayNameKo(),
                        action.getActionGroup() != null ? action.getActionGroup().name() : null,
                        roleResponses
                );
            }
            actionResponses.add(ar);
        }

        PromptRecommendationResponse.RecommendationMetadata metadata = new PromptRecommendationResponse.RecommendationMetadata(
                result.recommendationHints(),
                result.validationWarnings(),
                result.fallbackApplied(),
                result.requestMode() != null ? result.requestMode().name() : null
        );

        return new PromptRecommendationResponse(categoryBlock, intentBlock, actionResponses, metadata);
    }

    private List<RecommendedRoleResponse> toRoleResponses(
            List<RoleTypeInterface> roleCandidates,
            Map<String, RecommendationExplanation.RoleExplanationItem> roleExplanationByKey
    ) {
        if (roleCandidates == null || roleCandidates.isEmpty()) {
            return List.of();
        }
        return roleCandidates.stream()
                .map(r -> {
                    RecommendationExplanation.RoleExplanationItem ex = roleExplanationByKey.get(r.key());
                    if (ex != null) {
                        return RecommendedRoleResponse.withExplanation(
                                r.key(), r.getRoleNameKo(), ex.reason(), ex.source(), ex.orderingBasis()
                        );
                    }
                    return RecommendedRoleResponse.of(r.key(), r.getRoleNameKo());
                })
                .collect(Collectors.toList());
    }

    private static RecommendationExplanation toExplanationDto(RecommendationExplanationView view) {
        List<RecommendationExplanation.ActionExplanationItem> actionItems = view.actionExplanations().stream()
                .map(i -> new RecommendationExplanation.ActionExplanationItem(i.actionKey(), i.reason(), i.source(), i.orderingBasis()))
                .collect(Collectors.toList());
        List<RecommendationExplanation.RoleExplanationItem> roleItems = view.roleExplanations().stream()
                .map(i -> new RecommendationExplanation.RoleExplanationItem(i.roleKey(), i.reason(), i.source(), i.orderingBasis()))
                .collect(Collectors.toList());
        return new RecommendationExplanation(view.summaryHints(), actionItems, roleItems, view.fallbackIntentUsed(), view.fallbackReason());
    }

    private static String humanizeIntent(String intentName) {
        if (intentName == null || intentName.isEmpty()) return intentName;
        String lower = intentName.replace('_', ' ').toLowerCase();
        if (lower.isEmpty()) return lower;
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }
}
