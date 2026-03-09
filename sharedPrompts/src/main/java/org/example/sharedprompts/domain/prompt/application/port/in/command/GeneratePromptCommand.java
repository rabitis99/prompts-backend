package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.common.enums.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.ToneType;
import org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;

/**
 * 프롬프트 생성 유즈케이스 커맨드 — 사용자 입력만 포함한다.
 * Objective/Strategy/QualityPriority 등 내부 엔진 개념은 포함하지 않는다.
 *
 * <p>In the unified pipeline this command is built from {@link org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes}
 * by {@link org.example.sharedprompts.domain.prompt.application.service.orchestration.UnifiedPromptGenerationOrchestrator#toV2Command};
 * category, role, actionType, tone, style come from the axes, not inferred here.</p>
 *
 * <p>Logical semantic order: category (promptCategory) → role/actionType → tone/style → input, jsonSchema.
 * Do not infer semantics from these fields; use ConfirmedSemanticAxes as the single source of truth.</p>
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
        String jsonSchema
) {
    public GeneratePromptCommand {
        if (userId == null) throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        if (input == null || input.isBlank()) throw new IllegalArgumentException("input은 비어있을 수 없습니다.");

        isPublic = isPublic != null ? isPublic : false;
        tags = tags != null ? List.copyOf(tags) : List.of();

        // Do NOT default role or action; semantic resolution sets them or leaves null (no role section).
        tone = tone != null ? tone : ToneType.NEUTRAL;
        style = style != null ? style : StyleType.NARRATIVE;
        language = language != null ? language : LanguageType.KOREAN;
        experienceLevel = experienceLevel != null ? experienceLevel : ExperienceLevel.INTERMEDIATE;
    }
}
