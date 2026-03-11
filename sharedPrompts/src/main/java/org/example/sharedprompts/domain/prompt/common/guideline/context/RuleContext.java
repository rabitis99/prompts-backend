package org.example.sharedprompts.domain.prompt.common.guideline.context;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
public record RuleContext(
        TaskDomain taskDomain,
        String promptObjectiveName,
        ActionTypeInterface actionType,
        boolean requiresStructuredOutput,
        boolean hasJsonSchema,
        String userInput
) {
    public static RuleContext of(TaskDomain domain, String objectiveName, ActionTypeInterface actionType,
                                  boolean requiresStructuredOutput, boolean hasJsonSchema, String userInput) {
        return new RuleContext(
                domain,
                objectiveName != null ? objectiveName : "",
                actionType,
                requiresStructuredOutput,
                hasJsonSchema,
                userInput != null ? userInput : ""
        );
    }
}
