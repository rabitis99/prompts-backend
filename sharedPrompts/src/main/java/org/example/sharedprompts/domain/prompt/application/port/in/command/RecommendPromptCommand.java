package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.engine.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.experience.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.request.RequestMode;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.ActionIntent;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;

/** 추천 전용 커맨드. 생성 없이 추천 축만 반환 */
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
