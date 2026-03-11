package org.example.sharedprompts.domain.prompt.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.PromptCategory;

import java.time.Instant;
import java.util.List;

/**
 * 프롬프트 목록 조회 응답 DTO
 */
public record PromptSummaryResponse(

        Long id,

        String title,

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

        @JsonProperty("created_at")
        Instant createdAt
) {
}