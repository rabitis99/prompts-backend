package org.example.sharedprompts.dto.prompt.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.common.enums.PromptCategory;
import org.example.sharedprompts.dto.user.response.UserResponseDto;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResponseDto {

    private Long id;
    private String title;
    private String description;
    private String content;

    @JsonProperty("is_public")
    private boolean isPublic;

    @JsonProperty("prompt_category")
    private PromptCategory promptCategory;

    private List<String> tags;

    @JsonProperty("user_response_dto")
    private UserResponseDto userResponseDto;

    @JsonProperty("view_count")
    private Long viewCount;

    @JsonProperty("comment_count")
    private Long commentCount;

    @JsonProperty("like_count")
    private Long likeCount;

    @JsonProperty("favorite_count")
    private Long favoriteCount;

    /**
     * 엔티티 태그와 likeCount를 기반으로 DTO 생성.
     * 조회 모델에서는 {@link #fromWithTagNames(Prompt, List, Long)} 사용을 권장.
     */
    public static PromptResponseDto from(Prompt prompt, List<Tag> tags, Long likeCount) {
        List<String> tagNames = tags == null
                ? List.of()
                : tags.stream().map(Tag::getName).toList();

        return buildFrom(prompt, tagNames, likeCount);
    }

    /**
     * 태그 이름 기반 DTO 생성 (PromptTagQueryPort 등 조회 모델에서 사용).
     */
    public static PromptResponseDto fromWithTagNames(Prompt prompt, List<String> tagNames, Long likeCount) {
        return buildFrom(
                prompt,
                tagNames == null ? List.of() : tagNames.stream().toList(),
                likeCount
        );
    }

    private static PromptResponseDto buildFrom(Prompt prompt, List<String> tagNames, Long likeCount) {
        return PromptResponseDto.builder()
                .id(prompt.getId())
                .title(prompt.getTitle())
                .description(prompt.getDescription())
                .content(prompt.getContent())
                .isPublic(prompt.isPublic())
                .promptCategory(prompt.getPromptCategory())
                .tags(tagNames)
                .userResponseDto(prompt.getAuthor() != null ? UserResponseDto.from(prompt.getAuthor()) : null)
                .viewCount(prompt.getViewCount())
                .commentCount(prompt.getCommentCount())
                .likeCount(likeCount != null ? likeCount : 0L)
                .favoriteCount(prompt.getFavoriteCount())
                .build();
    }
}