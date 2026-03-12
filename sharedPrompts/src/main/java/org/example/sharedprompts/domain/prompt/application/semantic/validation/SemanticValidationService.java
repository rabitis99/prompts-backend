package org.example.sharedprompts.domain.prompt.application.semantic.validation;

import org.example.sharedprompts.domain.prompt.application.port.in.command.ConfirmedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.UnifiedGeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.command.RecommendPromptCommand;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionId;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.semantic.CategorySemanticProfile;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticFitLevel;
import org.example.sharedprompts.domain.prompt.domain.semantic.SemanticValidationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 카테고리·Intent 기반 시맨틱 검증. Action 호환성은 canonical 기준으로 비교. */
@Service
public class SemanticValidationService {

    private final CanonicalActionRegistry canonicalActionRegistry;

    public SemanticValidationService(CanonicalActionRegistry canonicalActionRegistry) {
        this.canonicalActionRegistry = canonicalActionRegistry;
    }

    private static final Set<String> INVALID_CODES = Set.of(
            "MISSING_PROFILE", "INVALID_INTENT_FOR_CATEGORY",
            "INTENT_FORBIDDEN_FOR_CATEGORY", "FORBIDDEN_COMBINATION"
    );

    public record ValidationInput(
            org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory category,
            ToneType tone,
            StyleType style,
            RoleTypeInterface roleType,
            ActionTypeInterface actionType,
            CategorySemanticProfile profile,
            ActionIntent resolvedIntent
    ) {}

    public SemanticValidationResult validate(
            UnifiedGeneratePromptCommand command,
            CategorySemanticProfile profile,
            ActionIntent resolvedIntent
    ) {
        return doValidate(new ValidationInput(
                command.category(),
                command.tone(),
                command.style(),
                command.roleType(),
                command.actionType(),
                profile,
                resolvedIntent
        ));
    }

    public SemanticValidationResult validate(
            RecommendPromptCommand command,
            CategorySemanticProfile profile,
            ActionIntent resolvedIntent
    ) {
        return doValidate(new ValidationInput(
                command.category(),
                command.tone(),
                command.style(),
                command.roleType(),
                command.actionType(),
                profile,
                resolvedIntent
        ));
    }

    public SemanticValidationResult validate(
            ConfirmedGeneratePromptCommand command,
            CategorySemanticProfile profile
    ) {
        return doValidate(new ValidationInput(
                command.category(),
                command.tone(),
                command.style(),
                command.roleType(),
                command.actionType(),
                profile,
                command.intent()
        ));
    }

    private SemanticValidationResult doValidate(ValidationInput input) {
        if (input.profile() == null) {
            return SemanticValidationResult.invalid(List.of(
                    new SemanticValidationResult.SemanticValidationItem(
                            "MISSING_PROFILE",
                            "No semantic profile for category: " + input.category(),
                            "category",
                            null
                    )));
        }

        List<SemanticValidationResult.SemanticValidationItem> items = new ArrayList<>();

        if (input.resolvedIntent() != null) {
            if (!input.profile().getAllowedIntents().contains(input.resolvedIntent())) {
                String fallbackHint = null;
                if (input.profile().getFallbackIntent() != null) {
                    fallbackHint = input.profile().getFallbackIntent().name();
                } else if (!input.profile().getFallbackCandidates().isEmpty()) {
                    fallbackHint = input.profile().getFallbackCandidates().get(0).intent().name();
                }
                items.add(new SemanticValidationResult.SemanticValidationItem(
                        "INVALID_INTENT_FOR_CATEGORY",
                        "Intent " + input.resolvedIntent() + " is not allowed for category " + input.profile().getCategory() + ". Use an intent from the category profile.",
                        "intent",
                        fallbackHint
                ));
            } else {
                SemanticFitLevel fitLevel = input.profile().getIntentFitLevel(input.resolvedIntent());
                if (fitLevel == SemanticFitLevel.FORBIDDEN) {
                    items.add(new SemanticValidationResult.SemanticValidationItem(
                            "INTENT_FORBIDDEN_FOR_CATEGORY",
                            "Intent " + input.resolvedIntent() + " is forbidden for category " + input.profile().getCategory() + ".",
                            "intent",
                            null
                    ));
                } else if (fitLevel == SemanticFitLevel.DISCOURAGED) {
                    items.add(new SemanticValidationResult.SemanticValidationItem(
                            "INTENT_DISCOURAGED",
                            "Intent " + input.resolvedIntent() + " is discouraged for " + input.profile().getCategory() + "; consider a preferred intent for better results.",
                            "intent",
                            null
                    ));
                }
            }
        }

        if (input.resolvedIntent() != null && input.tone() != null) {
            List<ToneType> discouraged = input.profile().getDiscouragedTonesForIntent(input.resolvedIntent());
            if (!discouraged.isEmpty() && discouraged.contains(input.tone())) {
                List<ToneType> preferredTones = input.profile().getPreferredTonesForIntent(input.resolvedIntent());
                String suggestion = preferredTones.isEmpty() ? null : preferredTones.get(0).name();
                String suggestionMsg = preferredTones.isEmpty() ? "" : " prefer " + suggestion + ".";
                items.add(new SemanticValidationResult.SemanticValidationItem(
                        "TONE_DISCOURAGED",
                        "Tone " + input.tone() + " is discouraged for " + input.profile().getCategory() + "+" + input.resolvedIntent() + ";" + suggestionMsg,
                        "tone",
                        suggestion
                ));
            }
        }

        if (input.resolvedIntent() != null && input.style() != null) {
            List<StyleType> discouraged = input.profile().getDiscouragedStylesForIntent(input.resolvedIntent());
            if (!discouraged.isEmpty() && discouraged.contains(input.style())) {
                List<StyleType> preferredStyles = input.profile().getPreferredStylesForIntent(input.resolvedIntent());
                String suggestion = preferredStyles.isEmpty() ? null : preferredStyles.get(0).name();
                String suggestionMsg = preferredStyles.isEmpty() ? "" : " prefer " + suggestion + ".";
                items.add(new SemanticValidationResult.SemanticValidationItem(
                        "STYLE_DISCOURAGED",
                        "Style " + input.style() + " is discouraged for " + input.profile().getCategory() + "+" + input.resolvedIntent() + ";" + suggestionMsg,
                        "style",
                        suggestion
                ));
            }
        }

        if (input.roleType() != null && input.resolvedIntent() != null) {
            List<RoleTypeInterface> recommended =
                    input.profile().getRecommendedRolesForIntent(input.resolvedIntent());
            if (!recommended.isEmpty()) {
                boolean compatible = recommended.stream().anyMatch(r -> sameRole(r, input.roleType()));
                if (!compatible) {
                    items.add(new SemanticValidationResult.SemanticValidationItem(
                            "ROLE_MAY_NOT_MATCH_INTENT",
                            "Selected role may not match intent " + input.resolvedIntent() + " for category " + input.profile().getCategory() + "; profile recommends roles for this branch.",
                            "role_type",
                            null
                    ));
                }
            }
        }

        if (input.actionType() != null && input.resolvedIntent() != null) {
            List<CanonicalActionId> compatibleCanonical = input.profile().getCompatibleCanonicalActionsForIntent(input.resolvedIntent());
            if (!compatibleCanonical.isEmpty()) {
                var inputCanonical = canonicalActionRegistry.toCanonical(input.actionType());
                boolean match = inputCanonical.isPresent() && compatibleCanonical.contains(inputCanonical.get());
                if (!match) {
                    items.add(new SemanticValidationResult.SemanticValidationItem(
                            "ACTION_MAY_NOT_MATCH_INTENT",
                            "Selected action may not match intent " + input.resolvedIntent() + " for category " + input.profile().getCategory() + "; profile recommends actions for this branch.",
                            "action_type",
                            null
                    ));
                }
            } else {
                List<ActionTypeInterface> compatible = input.profile().getCompatibleActionsForIntent(input.resolvedIntent());
                if (!compatible.isEmpty()) {
                    boolean match = compatible.stream().anyMatch(a -> canonicalActionRegistry.sameCanonicalCapability(a, input.actionType()));
                    if (!match) {
                        items.add(new SemanticValidationResult.SemanticValidationItem(
                                "ACTION_MAY_NOT_MATCH_INTENT",
                                "Selected action may not match intent " + input.resolvedIntent() + " for category " + input.profile().getCategory() + "; profile recommends actions for this branch.",
                                "action_type",
                                null
                        ));
                    }
                }
            }
        }

        if (input.resolvedIntent() != null && input.profile().isForbidden(
                input.resolvedIntent(),
                input.roleType(),
                input.actionType())) {
            items.add(new SemanticValidationResult.SemanticValidationItem(
                    "FORBIDDEN_COMBINATION",
                    "Category+intent+role+action combination is forbidden by the semantic profile.",
                    null,
                    null
            ));
        }

        if (items.stream().anyMatch(i -> INVALID_CODES.contains(i.code()))) {
            return SemanticValidationResult.invalid(items);
        }
        if (!items.isEmpty()) {
            return SemanticValidationResult.warning(items);
        }
        return SemanticValidationResult.success();
    }

    private static boolean sameRole(RoleTypeInterface a, RoleTypeInterface b) {
        return a != null && b != null && a.key().equals(b.key());
    }
}
