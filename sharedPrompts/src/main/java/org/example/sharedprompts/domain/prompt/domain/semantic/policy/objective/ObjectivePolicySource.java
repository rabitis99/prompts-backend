package org.example.sharedprompts.domain.prompt.domain.semantic.policy.objective;

import org.example.sharedprompts.domain.prompt.domain.value.objective.PromptObjective;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.util.Optional;

/**
 * Policy source for action → objective mapping.
 * Identifier-based lookup; source is swappable (in-memory / config / classpath / future DB).
 * Services depend on this interface, not on storage implementation.
 */
public interface ObjectivePolicySource {

    /**
     * Explicit objective for an action by its stable key.
     * Empty when no explicit mapping is defined.
     */
    Optional<PromptObjective> findByStableKey(String actionStableKey);

    /**
     * Default objective for a task domain when no action-specific mapping applies.
     */
    PromptObjective getDomainDefault(TaskDomain taskDomain);
}
