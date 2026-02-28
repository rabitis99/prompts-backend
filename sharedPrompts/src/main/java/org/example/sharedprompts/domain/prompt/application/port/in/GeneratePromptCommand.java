package org.example.sharedprompts.domain.prompt.application.port.in;

import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.action.EtcActionType;
import org.example.sharedprompts.domain.prompt.enums.role.EtcRoleType;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;

import java.util.List;

/**
 * 프롬프트 생성 유즈케이스 커맨드 — 사용자 입력만 포함한다.
 * Objective/Strategy/QualityPriority 등 내부 엔진 개념은 포함하지 않는다.
 */
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
        /** EXTRACTION 시 사용할 JSON Schema. null이면 팩토리 기본값 적용. */
        String jsonSchema
) {
    public GeneratePromptCommand {
        if (userId == null) throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        if (input == null || input.isBlank()) throw new IllegalArgumentException("input은 비어있을 수 없습니다.");

        isPublic = isPublic != null ? isPublic : false;
        tags = tags != null ? List.copyOf(tags) : List.of();

        actionType = actionType != null ? actionType : EtcActionType.GENERAL_CONSULTATION;
        roleType = roleType != null ? roleType : EtcRoleType.GENERAL_CONSULTANT;

        tone = tone != null ? tone : ToneType.NEUTRAL;
        style = style != null ? style : StyleType.NARRATIVE;
        language = language != null ? language : LanguageType.KOREAN;
        experienceLevel = experienceLevel != null ? experienceLevel : ExperienceLevel.INTERMEDIATE;
    }
}
