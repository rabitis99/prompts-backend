package org.example.sharedprompts.domain.prompt.application.port.in.command;

import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;

import java.util.List;
import java.util.Objects;

/** 확정 축 기반 생성 커맨드 */
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
        Objects.requireNonNull(tone, "tone must not be null");
        Objects.requireNonNull(style, "style must not be null");
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(experience, "experience must not be null");
        if (tags == null) {
            tags = List.of();
        } else {
            if (tags.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("tags must not contain null values");
            }
            tags = List.copyOf(tags);
        }
    }
}
