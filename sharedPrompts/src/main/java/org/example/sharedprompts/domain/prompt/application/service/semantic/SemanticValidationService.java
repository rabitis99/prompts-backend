package org.example.sharedprompts.domain.prompt.application.service.semantic;

import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfileRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticFitLevel;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticValidationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Category-aware semantic validation.
 * Produces VALID, VALID_WITH_WARNING, or INVALID using intent fit level, discouraged tone/style,
 * role/action compatibility, and forbidden combinations.
 */
@Service
public class SemanticValidationService {

    private final CategorySemanticProfileRegistry profileRegistry;

    public SemanticValidationService(CategorySemanticProfileRegistry profileRegistry) {
        this.profileRegistry = profileRegistry;
    }

    /**
     * Validates the command against the given profile.
     * Returns valid(), invalid(), or warning() with structured items.
     * - FORBIDDEN intent or forbidden combination → INVALID
     * - DISCOURAGED intent or discouraged tone/style or role/action mismatch → VALID_WITH_WARNING
     */
    public SemanticValidationResult validate(
            UnifiedGeneratePromptCommand command,
            CategorySemanticProfile profile,
            ActionIntent resolvedIntent
    ) {
        if (profile == null) {
            return SemanticValidationResult.invalid(List.of(
                    new SemanticValidationResult.SemanticValidationItem(
                            "MISSING_PROFILE",
                            "No semantic profile for category: " + command.category(),
                            "category",
                            null
                    )));
        }

        List<SemanticValidationResult.SemanticValidationItem> items = new ArrayList<>();

        // 1) Intent allowed and fit level
        if (resolvedIntent != null) {
            if (!profile.getAllowedIntents().contains(resolvedIntent)) {
                String fallbackHint = null;
                if (profile.getFallbackIntent() != null) {
                    fallbackHint = profile.getFallbackIntent().name();
                } else if (!profile.getFallbackCandidates().isEmpty()) {
                    fallbackHint = profile.getFallbackCandidates().get(0).intent().name();
                }
                items.add(new SemanticValidationResult.SemanticValidationItem(
                        "INVALID_INTENT_FOR_CATEGORY",
                        "Intent " + resolvedIntent + " is not allowed for category " + profile.getCategory() + ". Use an intent from the category profile.",
                        "intent",
                        fallbackHint
                ));
            } else {
                SemanticFitLevel fitLevel = profile.getIntentFitLevel(resolvedIntent);
                if (fitLevel == SemanticFitLevel.FORBIDDEN) {
                    items.add(new SemanticValidationResult.SemanticValidationItem(
                            "INTENT_FORBIDDEN_FOR_CATEGORY",
                            "Intent " + resolvedIntent + " is forbidden for category " + profile.getCategory() + ".",
                            "intent",
                            null
                    ));
                } else if (fitLevel == SemanticFitLevel.DISCOURAGED) {
                    items.add(new SemanticValidationResult.SemanticValidationItem(
                            "INTENT_DISCOURAGED",
                            "Intent " + resolvedIntent + " is discouraged for " + profile.getCategory() + "; consider a preferred intent for better results.",
                            "intent",
                            null
                    ));
                }
            }
        }

        // 2) Discouraged tone for (category, intent)
        if (resolvedIntent != null && command.tone() != null) {
            List<ToneType> discouraged = profile.getDiscouragedTonesForIntent(resolvedIntent);
            if (!discouraged.isEmpty() && discouraged.contains(command.tone())) {
                items.add(new SemanticValidationResult.SemanticValidationItem(
                        "TONE_DISCOURAGED",
                        "Tone " + command.tone() + " is discouraged for " + profile.getCategory() + "+" + resolvedIntent + "; analytical tasks often work better with NEUTRAL or PROFESSIONAL.",
                        "tone",
                        ToneType.NEUTRAL.name()
                ));
            }
        }

        // 3) Discouraged style for (category, intent)
        if (resolvedIntent != null && command.style() != null) {
            List<StyleType> discouraged = profile.getDiscouragedStylesForIntent(resolvedIntent);
            if (!discouraged.isEmpty() && discouraged.contains(command.style())) {
                items.add(new SemanticValidationResult.SemanticValidationItem(
                        "STYLE_DISCOURAGED",
                        "Style " + command.style() + " is discouraged for " + profile.getCategory() + "+" + resolvedIntent + "; prefer TECHNICAL or CONCISE for this branch.",
                        "style",
                        StyleType.TECHNICAL.name()
                ));
            }
        }

        // 4) Role valid for category+intent (if provided)
        if (command.roleType() != null && resolvedIntent != null) {
            List<RoleTypeInterface> recommended =
                    profile.getRecommendedRolesForIntent(resolvedIntent);
            if (!recommended.isEmpty()) {
                boolean compatible = recommended.stream().anyMatch(r -> sameRole(r, command.roleType()));
                if (!compatible) {
                    items.add(new SemanticValidationResult.SemanticValidationItem(
                            "ROLE_MAY_NOT_MATCH_INTENT",
                            "Selected role may not match intent " + resolvedIntent + " for category " + profile.getCategory() + "; profile recommends roles for this branch.",
                            "role_type",
                            null
                    ));
                }
            }
        }

        // 5) Action valid for category+intent (if provided)
        if (command.actionType() != null && resolvedIntent != null) {
            List<ActionTypeInterface> compatible =
                    profile.getCompatibleActionsForIntent(resolvedIntent);
            if (!compatible.isEmpty()) {
                boolean match = compatible.stream().anyMatch(a -> sameAction(a, command.actionType()));
                if (!match) {
                    items.add(new SemanticValidationResult.SemanticValidationItem(
                            "ACTION_MAY_NOT_MATCH_INTENT",
                            "Selected action may not match intent " + resolvedIntent + " for category " + profile.getCategory() + "; profile recommends actions for this branch.",
                            "action_type",
                            null
                    ));
                }
            }
        }

        // 6) Forbidden combination
        if (resolvedIntent != null && profile.isForbidden(
                resolvedIntent,
                command.roleType(),
                command.actionType())) {
            items.add(new SemanticValidationResult.SemanticValidationItem(
                    "FORBIDDEN_COMBINATION",
                    "Category+intent+role+action combination is forbidden by the semantic profile.",
                    null,
                    null
            ));
        }

        if (items.stream().anyMatch(i -> "MISSING_PROFILE".equals(i.code()) || "INVALID_INTENT_FOR_CATEGORY".equals(i.code()) || "INTENT_FORBIDDEN_FOR_CATEGORY".equals(i.code()) || "FORBIDDEN_COMBINATION".equals(i.code()))) {
            return SemanticValidationResult.invalid(items);
        }
        if (!items.isEmpty()) {
            return SemanticValidationResult.warning(items);
        }
        return SemanticValidationResult.valid();
    }

    private static boolean sameRole(RoleTypeInterface a, RoleTypeInterface b) {
        return a != null && b != null && a.key().equals(b.key());
    }

    private static boolean sameAction(ActionTypeInterface a, ActionTypeInterface b) {
        return a != null && b != null && a.key().equals(b.key());
    }
}
