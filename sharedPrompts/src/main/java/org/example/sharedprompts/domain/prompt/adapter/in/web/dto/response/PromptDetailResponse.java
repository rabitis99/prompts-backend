package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;

import java.time.Instant;
import java.util.List;

/**
 * /prompts/{id} 상세 조회용 응답 DTO.
 */
public record PromptDetailResponse(
        Long id,
        String title,
        String description,
        String content,
        @JsonProperty("prompt_category")
        PromptCategory promptCategory,
        List<String> tags,
        @JsonProperty("author_id")
        Long authorId,
        @JsonProperty("author_nickname")
        String authorNickname,
        @JsonProperty("like_count")
        Long likeCount,
        @JsonProperty("view_count")
        Long viewCount,
        @JsonProperty("is_public")
        boolean isPublic,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt
) {
}

