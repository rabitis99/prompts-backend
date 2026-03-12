package org.example.sharedprompts.domain.prompt.domain.semantic;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionId;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Category-aware semantic profile.
 * Defines intent fit levels (PREFERRED / ALLOWED / DISCOURAGED / FORBIDDEN), per-intent recommended roles,
 * compatible actions, preferred or discouraged tones/styles, and fallback behavior for a {@link PromptCategory}.
 *
 * <p>Structure supports validation severity (VALID, VALID_WITH_WARNING, INVALID) and
 * recommendation rationale without changing the pipeline contract.</p>
 */
public interface CategorySemanticProfile {

    PromptCategory getCategory();

    TaskDomain getBaseTaskDomain();

    /**
     * Intents allowed for this category (fit level is not FORBIDDEN).
     * Engine must not infer intent from category alone; intent is resolved first, then validated.
     */
    Set<ActionIntent> getAllowedIntents();

    /**
     * Semantic fit level for this intent in this category.
     * PREFERRED = natural fit; ALLOWED = valid; DISCOURAGED = valid but suboptimal; FORBIDDEN = invalid.
     * If intent is not in allowed set, returns FORBIDDEN.
     */
    default SemanticFitLevel getIntentFitLevel(ActionIntent intent) {
        if (intent == null || !getAllowedIntents().contains(intent)) {
            return SemanticFitLevel.FORBIDDEN;
        }
        return SemanticFitLevel.ALLOWED;
    }

    /**
     * Recommended roles for (category, intent). Order may imply preference (first = preferred).
     * May be empty; engine must not invent role when null.
     */
    List<RoleTypeInterface> getRecommendedRolesForIntent(ActionIntent intent);

    /**
     * Compatible action types for (category, intent). Used for validation and recommendation.
     * Concrete list is retained for API/response (e.g. recommendation candidates); internal
     * compatibility checks prefer {@link #getCompatibleCanonicalActionsForIntent(ActionIntent)}.
     */
    List<ActionTypeInterface> getCompatibleActionsForIntent(ActionIntent intent);

    /**
     * Compatible canonical actions for (category, intent). Primary internal capability layer;
     * validation and recommendation use this for capability matching. Default returns empty
     * so profiles without canonical data remain valid.
     */
    default List<CanonicalActionId> getCompatibleCanonicalActionsForIntent(ActionIntent intent) {
        return Collections.emptyList();
    }

    /**
     * Preferred tone types for (category, intent). Optional constraint.
     */
    default List<ToneType> getPreferredTonesForIntent(ActionIntent intent) {
        return Collections.emptyList();
    }

    /**
     * Preferred style types for (category, intent). Optional constraint.
     */
    default List<StyleType> getPreferredStylesForIntent(ActionIntent intent) {
        return Collections.emptyList();
    }

    /**
     * Discouraged tone types for (category, intent). If user selects one, validation may add VALID_WITH_WARNING.
     */
    default List<ToneType> getDiscouragedTonesForIntent(ActionIntent intent) {
        return Collections.emptyList();
    }

    /**
     * Discouraged style types for (category, intent). If user selects one, validation may add VALID_WITH_WARNING.
     */
    default List<StyleType> getDiscouragedStylesForIntent(ActionIntent intent) {
        return Collections.emptyList();
    }

    /**
     * Forbidden or unnatural combinations (e.g. category + intent + role). Empty if none.
     */
    default boolean isForbidden(ActionIntent intent, RoleTypeInterface role, ActionTypeInterface action) {
        return false;
    }

    /**
     * Fallback intent when user did not specify one (e.g. for EXTRACTION request_type).
     * Return null to require explicit intent.
     */
    default ActionIntent getFallbackIntent() {
        return null;
    }

    /**
     * Optional fallback candidates with rationale (for correction suggestions).
     * Default returns single fallback intent if present.
     */
    default List<FallbackCandidate> getFallbackCandidates() {
        ActionIntent fallback = getFallbackIntent();
        if (fallback == null) return List.of();
        return List.of(new FallbackCandidate(fallback, "Default intent for " + getCategory().name()));
    }
}
