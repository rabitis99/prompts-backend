package org.example.sharedprompts.domain.prompt.domain.semantic.policy.role;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;

/**
 * Policy source for preferred roles per (category, intent) or (category, intent, action).
 * Enables data-driven role recommendation; profile can delegate to this when present.
 * Source is swappable; UX keys (Category, Intent, Action) are first-class.
 */
public interface RolePreferenceSource {

    /**
     * Preferred role stable keys for (category, intent). Order implies preference.
     * Empty when no data; consumer uses profile or other fallback.
     */
    List<String> getPreferredRoleKeys(PromptCategory category, ActionIntent intent);

    /**
     * Preferred role stable keys for (category, intent, actionKey). Optional refinement.
     * Default returns same as getPreferredRoleKeys(category, intent) when not overridden.
     */
    default List<String> getPreferredRoleKeys(PromptCategory category, ActionIntent intent, String actionKey) {
        return getPreferredRoleKeys(category, intent);
    }
}
