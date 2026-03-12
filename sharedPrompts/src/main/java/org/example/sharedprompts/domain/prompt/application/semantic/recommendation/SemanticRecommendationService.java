package org.example.sharedprompts.domain.prompt.application.semantic.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionId;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDefinition;
import org.example.sharedprompts.domain.prompt.domain.semantic.IntentDictionary;
import org.example.sharedprompts.domain.prompt.domain.semantic.RecommendationResult;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticFitLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 카테고리·Intent 기반 역할/액션 추천·힌트. Action 호환성은 canonical 기준으로 비교. */
@Service
public class SemanticRecommendationService {

    private final CanonicalActionRegistry canonicalActionRegistry;

    public SemanticRecommendationService(CanonicalActionRegistry canonicalActionRegistry) {
        this.canonicalActionRegistry = canonicalActionRegistry;
    }

    public RecommendationResult recommend(
            PromptCategory category,
            ActionIntent intent,
            CategorySemanticProfile profile,
            RoleTypeInterface userProvidedRole,
            ActionTypeInterface userProvidedAction,
            boolean fallbackIntentWasUsed
    ) {
        if (profile == null || intent == null) {
            return new RecommendationResult(
                    category,
                    intent,
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of(),
                    List.of("No profile or intent for recommendation.")
            );
        }

        List<RoleTypeInterface> roleCandidates = profile.getRecommendedRolesForIntent(intent);
        List<ActionTypeInterface> actionCandidates = profile.getCompatibleActionsForIntent(intent);

        Optional<RoleTypeInterface> recommendedRole = Optional.empty();
        if (userProvidedRole != null) {
            recommendedRole = Optional.of(userProvidedRole);
        } else if (!roleCandidates.isEmpty()) {
            recommendedRole = Optional.of(roleCandidates.get(0));
        }

        Optional<ActionTypeInterface> recommendedAction = Optional.empty();
        if (userProvidedAction != null) {
            recommendedAction = Optional.of(userProvidedAction);
        } else if (!actionCandidates.isEmpty()) {
            recommendedAction = Optional.of(actionCandidates.get(0));
        }

        List<String> hints = buildHints(
                category, intent, profile,
                userProvidedRole, userProvidedAction,
                recommendedRole.orElse(null), recommendedAction.orElse(null),
                roleCandidates, actionCandidates,
                fallbackIntentWasUsed
        );

        return new RecommendationResult(
                category,
                intent,
                recommendedRole,
                recommendedAction,
                roleCandidates,
                actionCandidates,
                hints
        );
    }

    public RecommendationResult recommend(
            PromptCategory category,
            ActionIntent intent,
            CategorySemanticProfile profile,
            RoleTypeInterface userProvidedRole,
            ActionTypeInterface userProvidedAction
    ) {
        return recommend(category, intent, profile, userProvidedRole, userProvidedAction, false);
    }

    private List<String> buildHints(
            PromptCategory category,
            ActionIntent intent,
            CategorySemanticProfile profile,
            RoleTypeInterface userProvidedRole,
            ActionTypeInterface userProvidedAction,
            RoleTypeInterface recommendedRole,
            ActionTypeInterface recommendedAction,
            List<RoleTypeInterface> roleCandidates,
            List<ActionTypeInterface> actionCandidates,
            boolean fallbackIntentWasUsed
    ) {
        List<String> hints = new ArrayList<>();

        if (fallbackIntentWasUsed) {
            Optional<IntentDefinition> def = IntentDictionary.get(intent);
            String why = def.map(d -> "Fallback: " + d.canonicalMeaning()).orElse("Intent was omitted; profile fallback applied.");
            hints.add(why);
        }

        SemanticFitLevel fitLevel = profile.getIntentFitLevel(intent);
        if (fitLevel == SemanticFitLevel.PREFERRED) {
            IntentDictionary.get(intent).ifPresent(d ->
                    hints.add("Intent " + intent + " is preferred for " + category + ": " + d.canonicalMeaning())
            );
        } else if (fitLevel == SemanticFitLevel.DISCOURAGED) {
            hints.add("Intent " + intent + " is allowed but discouraged for " + category + "; consider a preferred intent for this branch.");
        }

        if (userProvidedRole == null && !roleCandidates.isEmpty() && recommendedRole != null) {
            Optional<IntentDefinition> def = IntentDictionary.get(intent);
            if (def.isPresent() && !def.get().representativeRoleHints().isEmpty()) {
                hints.add("Role recommended for " + intent + ": " + String.join(", ", def.get().representativeRoleHints().stream().limit(2).toList()) + ".");
            } else {
                hints.add("Role recommended from category+intent; override with role_type if needed.");
            }
        } else if (userProvidedRole != null && !roleCandidates.isEmpty()) {
            boolean match = roleCandidates.stream().anyMatch(r -> r.key().equals(userProvidedRole.key()));
            if (!match) {
                hints.add("Selected role is not in the preferred set for " + category + "+" + intent + "; consider profile-recommended roles for best fit.");
            }
        }

        if (userProvidedAction == null && !actionCandidates.isEmpty() && recommendedAction != null) {
            Optional<IntentDefinition> def = IntentDictionary.get(intent);
            if (def.isPresent() && !def.get().representativeActionHints().isEmpty()) {
                hints.add("Action fits " + intent + ": " + String.join(", ", def.get().representativeActionHints().stream().limit(2).toList()) + ".");
            } else {
                hints.add("Action recommended from category+intent; override with action_type if needed.");
            }
        } else if (userProvidedAction != null && !actionCandidates.isEmpty()) {
            List<CanonicalActionId> compatibleCanonical = profile.getCompatibleCanonicalActionsForIntent(intent);
            boolean match = !compatibleCanonical.isEmpty()
                    ? canonicalActionRegistry.toCanonical(userProvidedAction).map(compatibleCanonical::contains).orElse(false)
                    : actionCandidates.stream().anyMatch(a -> canonicalActionRegistry.sameCanonicalCapability(a, userProvidedAction));
            if (!match) {
                hints.add("Selected action may not match " + intent + " for " + category + "; profile recommends compatible actions for this branch.");
            }
        }

        return hints;
    }
}
