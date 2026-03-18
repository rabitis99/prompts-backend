package org.example.sharedprompts.domain.prompt.application.semantic.policy;

import java.util.Set;

/**
 * Context for semantic validation: known stable keys (category, intent, action, group, role, objective).
 * Validators use this to check that document references exist; no enum/class names.
 */
public interface PolicyValidationContext {

    /** Valid category stable keys (e.g. PROMPT_CATEGORY.WRITING). */
    Set<String> knownCategoryKeys();

    /** Valid intent keys (e.g. GENERATE, EXPLAIN). */
    Set<String> knownIntentKeys();

    /** Valid action stable keys. */
    Set<String> knownActionKeys();

    /** Valid action group stable keys (e.g. CODE_GENERATION). */
    Set<String> knownActionGroupKeys();

    /** Valid role stable keys. */
    Set<String> knownRoleKeys();

    /** Valid objective names (e.g. REASONING, ANALYTICAL). */
    Set<String> knownObjectiveNames();

    /** Valid task domain names for domainDefaults. */
    Set<String> knownTaskDomainNames();
}
