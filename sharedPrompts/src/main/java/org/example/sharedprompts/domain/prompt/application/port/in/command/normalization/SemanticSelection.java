package org.example.sharedprompts.domain.prompt.application.port.in.command.normalization;

import org.example.sharedprompts.domain.prompt.common.enums.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

/**
 * Semantic selection axis: category → intent → role/action.
 * Used when normalizing requests so the internal model does not behave as a flat enum bag.
 * Compatibility and allowed combinations live in CategorySemanticProfile and IntentDictionary.
 */
public record SemanticSelection(
        PromptCategory category,
        ActionIntent intent,
        RoleTypeInterface roleType,
        ActionTypeInterface actionType
) {}
