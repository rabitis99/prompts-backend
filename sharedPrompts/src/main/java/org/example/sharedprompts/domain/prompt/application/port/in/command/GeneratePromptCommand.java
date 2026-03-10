package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;

/** 프롬프트 생성 커맨드 */
public record GeneratePromptCommand(
        Long userId,
        String title,
        String description,
        Boolean isPublic,
        PromptCategory promptCategory,
        List<String> tags,
        String input,
        ActionTypeInterface actionType,
        RoleTypeInterface roleType,
        ToneType tone,
        StyleType style,
        LanguageType language,
        ExperienceLevel experienceLevel,
        boolean experimentalEnabled,
        String jsonSchema
) {
    public GeneratePromptCommand {
        if (userId == null) throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        if (input == null || input.isBlank()) throw new IllegalArgumentException("input은 비어있을 수 없습니다.");

        isPublic = isPublic != null ? isPublic : false;
        tags = tags != null ? List.copyOf(tags) : List.of();

        tone = tone != null ? tone : ToneType.NEUTRAL;
        style = style != null ? style : StyleType.NARRATIVE;
        language = language != null ? language : LanguageType.KOREAN;
        experienceLevel = experienceLevel != null ? experienceLevel : ExperienceLevel.INTERMEDIATE;
    }
}
