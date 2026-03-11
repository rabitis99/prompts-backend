package org.example.sharedprompts.domain.prompt.application.prompt.command;

import java.util.List;

/** 프롬프트 수정 페이로드 (Validator·Service 간 전달) */
public record UpdatePromptPayload(
        String title,
        String description,
        Boolean isPublic,
        List<String> tags,
        String content
) {
    public UpdatePromptPayload {
        tags = tags == null ? null : List.copyOf(tags);
    }
}
