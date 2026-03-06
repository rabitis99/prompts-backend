package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;

import java.time.Instant;
import java.util.List;

/**
 * 목록 조회 시 사용하는 프롬프트 요약 뷰 모델.
 */
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
}

