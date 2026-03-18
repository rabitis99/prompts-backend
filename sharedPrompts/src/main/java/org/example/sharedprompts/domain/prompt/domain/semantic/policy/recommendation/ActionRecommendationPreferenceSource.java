package org.example.sharedprompts.domain.prompt.domain.semantic.policy.recommendation;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

/**
 * Provides explicit preferred action order for (category, intent).
 * Stable key list order defines recommendation order; fallback when not listed
 * is defined by {@link ActionRecommendationOrderPolicy}.
 */
public interface ActionRecommendationPreferenceSource {

    /**
     * Preferred action stable keys for (category, intent), in order.
     * Only keys that appear here get explicit position; others are ordered by fallback.
     */
    List<String> getPreferredActionKeys(PromptCategory category, ActionIntent intent);
}
