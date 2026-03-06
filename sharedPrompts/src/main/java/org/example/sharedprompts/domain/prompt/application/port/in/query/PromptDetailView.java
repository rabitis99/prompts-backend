package org.example.sharedprompts.domain.prompt.application.port.in.query;

import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;

import java.time.Instant;
import java.util.List;

/**
 * 단건 상세 조회 시 사용하는 프롬프트 상세 뷰 모델.
 */
public record PromptDetailView(
        Long id,
        String title,
        String description,
        String content,
        PromptCategory category,
        List<String> tags,
        Long authorId,
        String authorNickname,
        Long likeCount,
        Long viewCount,
        boolean isPublic,
        Instant createdAt,
        Instant updatedAt
) {
}

