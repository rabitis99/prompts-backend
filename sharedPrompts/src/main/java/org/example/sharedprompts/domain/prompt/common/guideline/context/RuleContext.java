package org.example.sharedprompts.domain.prompt.common.guideline.context;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.CanonicalActionId;

/**
 * Context for guideline rule applicability and prompt assembly.
 * Prefer {@link #canonicalActionId()} for capability-based decisions; {@link #actionType()} is the
 * concrete (external) value for compatibility and metadata.
 */
public record RuleContext(
        TaskDomain taskDomain,
        String promptObjectiveName,
        ActionTypeInterface actionType,
        CanonicalActionId canonicalActionId,
        boolean requiresStructuredOutput,
        boolean hasJsonSchema,
        String userInput
) {
    public static RuleContext of(TaskDomain domain, String objectiveName, ActionTypeInterface actionType,
                                  boolean requiresStructuredOutput, boolean hasJsonSchema, String userInput) {
        return of(domain, objectiveName, actionType, null, requiresStructuredOutput, hasJsonSchema, userInput);
    }

    /** Canonical-first: use when capability drives rule selection or prompt assembly. */
    public static RuleContext of(TaskDomain domain, String objectiveName, ActionTypeInterface actionType,
                                  CanonicalActionId canonicalActionId,
                                  boolean requiresStructuredOutput, boolean hasJsonSchema, String userInput) {
        return new RuleContext(
                domain,
                objectiveName != null ? objectiveName : "",
                actionType,
                canonicalActionId,
                requiresStructuredOutput,
                hasJsonSchema,
                userInput != null ? userInput : ""
        );
    }
}
