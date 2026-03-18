package org.example.sharedprompts.domain.prompt.domain.semantic.policy.compatibility;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.util.List;

/**
 * Policy source for category–intent compatible action groups (stable group keys).
 * Enables gradual data-driven compatibility; profile registry can assemble profiles from this source.
 * Source is swappable; services depend on interface only.
 */
public interface CompatibilityPolicySource {

    /**
     * Compatible action group stable keys for (category, intent).
     * Empty list when no data; profile can fall back to built-in definition.
     */
    List<String> getCompatibleGroupKeys(PromptCategory category, ActionIntent intent);
}
