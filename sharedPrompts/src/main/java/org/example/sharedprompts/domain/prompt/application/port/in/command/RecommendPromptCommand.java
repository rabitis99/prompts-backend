package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

/**
 * Command for the recommendation-only flow.
 * Used by POST /prompts/recommend to return recommended semantic axes without generating a prompt.
 */
public record RecommendPromptCommand(
        RequestMode requestMode,
        PromptCategory category,
        ActionIntent intent,
        RoleTypeInterface roleType,
        ActionTypeInterface actionType,
        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experience,
        String rawInput
) {
    public RecommendPromptCommand {
        tone = tone != null ? tone : ToneType.NEUTRAL;
        style = style != null ? style : StyleType.NARRATIVE;
        language = language != null ? language : LanguageType.KOREAN;
        experience = experience != null ? experience : ExperienceLevel.INTERMEDIATE;
    }
}
