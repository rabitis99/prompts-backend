package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;

import java.time.Instant;
import java.util.List;

/** 프롬프트 목록 요약 뷰 */
public record PromptSummaryView(
        Long id,
        String title,
        PromptCategory category,
        List<String> tags,
        Long authorId,
        String authorNickname,
        Long likeCount,
        Long viewCount,
        Instant createdAt
) {
    public PromptSummaryView {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}

