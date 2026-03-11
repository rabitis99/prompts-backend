package org.example.sharedprompts.domain.prompt.application.port.in.command;

import java.util.List;

/** 프롬프트 수정 커맨드. 웹 DTO와 분리 */
public record UpdatePromptCommand(
        Long promptId,
        Long userId,
        String title,
        String description,
        Boolean isPublic,
        List<String> tags,
        String content
) {
    public UpdatePromptCommand {
        if (promptId == null) throw new IllegalArgumentException("promptId는 null일 수 없습니다.");
        if (userId == null) throw new IllegalArgumentException("userId는 null일 수 없습니다.");
        tags = tags != null ? List.copyOf(tags) : null;
    }
}

