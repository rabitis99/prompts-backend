package org.example.sharedprompts.domain.prompt.common.guideline.context;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.canonical.ActionGroup;

/**
 * Context for guideline rule applicability and prompt assembly.
 * Prefer {@link #actionGroup()} for capability-based decisions; {@link #actionType()} is the
 * concrete (external) value for compatibility and metadata.
 */
public record RuleContext(
        TaskDomain taskDomain,
        String promptObjectiveName,
        ActionTypeInterface actionType,
        ActionGroup actionGroup,
        boolean requiresStructuredOutput,
        boolean hasJsonSchema,
        String userInput
) {
    public static RuleContext of(TaskDomain domain, String objectiveName, ActionTypeInterface actionType,
                                  boolean requiresStructuredOutput, boolean hasJsonSchema, String userInput) {
        return of(
                domain,
                objectiveName,
                actionType,
                actionType != null ? actionType.getActionGroup() : null,
                requiresStructuredOutput,
                hasJsonSchema,
                userInput
        );
    }

    /**
     * Action-group-first: use when capability drives rule selection or prompt assembly.
     * Precedence: explicit actionGroup, then actionType.getActionGroup() as fallback.
     */
    public static RuleContext of(TaskDomain domain, String objectiveName, ActionTypeInterface actionType,
                                  ActionGroup actionGroup,
                                  boolean requiresStructuredOutput, boolean hasJsonSchema, String userInput) {
        ActionGroup resolvedActionGroup =
                actionGroup != null ? actionGroup : (actionType != null ? actionType.getActionGroup() : null);
        return new RuleContext(
                domain,
                objectiveName != null ? objectiveName : "",
                actionType,
                resolvedActionGroup,
                requiresStructuredOutput,
                hasJsonSchema,
                userInput != null ? userInput : ""
        );
    }
}
