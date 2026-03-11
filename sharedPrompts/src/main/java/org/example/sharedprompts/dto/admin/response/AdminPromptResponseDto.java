package org.example.sharedprompts.dto.admin.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class AdminPromptResponseDto {

    private Long id;
    private String title;

    @JsonProperty("prompt_category")
    private PromptCategory promptCategory;

    @JsonProperty("is_public")
    private boolean isPublic;

    @JsonProperty("author_id")
    private Long authorId;

    @JsonProperty("author_nickname")
    private String authorNickname;

    @JsonProperty("view_count")
    private long viewCount;

    @JsonProperty("comment_count")
    private long commentCount;

    @JsonProperty("like_count")
    private long likeCount;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    /** LikeCountPort 등에서 전달된 likeCount를 사용해 DTO 생성 (엔티티 스냅샷 값 사용하지 않음). */
    public static AdminPromptResponseDto from(Prompt prompt, long likeCount) {
        return AdminPromptResponseDto.builder()
                .id(prompt.getId())
                .title(prompt.getTitle())
                .promptCategory(prompt.getPromptCategory())
                .isPublic(prompt.isPublic())
                .authorId(prompt.getAuthor().getId())
                .authorNickname(prompt.getAuthor().getNickname())
                .viewCount(prompt.getViewCount())
                .commentCount(prompt.getCommentCount())
                .likeCount(likeCount)
                .createdAt(prompt.getCreatedAt())
                .updatedAt(prompt.getUpdatedAt())
                .build();
    }
}