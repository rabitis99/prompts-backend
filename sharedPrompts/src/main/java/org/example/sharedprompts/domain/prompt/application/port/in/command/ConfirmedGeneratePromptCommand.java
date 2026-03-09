package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;

/**
 * Command for generating a prompt from already-confirmed semantic axes (POST /prompts/generate/confirmed).
 * Objective and outputNeeds are derived from IntentDictionary in the service, not from the request.
 * <p>{@code userId} is an extension for auth context (not in original doc); ensure it does not change domain rules or API contract.</p>
 */
public record ConfirmedGeneratePromptCommand(
        Long userId,
        RequestMode requestMode,
        PromptCategory category,
        ActionIntent intent,
        RoleTypeInterface roleType,
        ActionTypeInterface actionType,
        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experience,
        String input,
        String jsonSchema,
        String title,
        String description,
        List<String> tags
) {
    public ConfirmedGeneratePromptCommand {
        tone = tone != null ? tone : ToneType.NEUTRAL;
        style = style != null ? style : StyleType.NARRATIVE;
        language = language != null ? language : LanguageType.KOREAN;
        experience = experience != null ? experience : ExperienceLevel.INTERMEDIATE;
        tags = tags != null ? List.copyOf(tags) : List.of();
    }
}
