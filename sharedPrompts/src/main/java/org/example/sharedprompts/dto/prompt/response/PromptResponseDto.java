package org.example.sharedprompts.dto.prompt.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.tag.Tag;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
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


    public static PromptResponseDto from(Prompt prompt, List<Tag> tags) {
        return from(prompt, tags, prompt.getLikeCount());
    }

    public static PromptResponseDto from(Prompt prompt, List<Tag> tags, Long likeCount) {

        List<String> tagNames = tags == null
                ? List.of()
                : tags.stream().map(Tag::getName).toList();

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
                .likeCount(likeCount)
                .favoriteCount(prompt.getFavoriteCount())
                .build();
    }
}
