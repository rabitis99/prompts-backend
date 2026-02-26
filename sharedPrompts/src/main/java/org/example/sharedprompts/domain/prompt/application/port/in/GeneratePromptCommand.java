package org.example.sharedprompts.domain.prompt.application.port.in;

import org.example.sharedprompts.domain.prompt.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.domain.prompt.enums.StyleType;
import org.example.sharedprompts.domain.prompt.enums.ToneType;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
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
        boolean experimentalEnabled
) {
    public GeneratePromptCommand {
        if (userId == null) throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        if (input == null || input.isBlank()) throw new IllegalArgumentException("input은 비어있을 수 없습니다.");
        tags = tags != null ? List.copyOf(tags) : List.of();
    }
}
