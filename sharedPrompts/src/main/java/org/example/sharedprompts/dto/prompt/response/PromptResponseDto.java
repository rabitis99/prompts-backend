package org.example.sharedprompts.dto.prompt.response;

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
    private boolean isPublic;
    private PromptCategory promptCategory;
    private List<String> tags;
    private UserResponseDto userResponseDto;
    private long viewCount;
    private long commentCount;


    public static PromptResponseDto from(Prompt prompt, List<Tag> tags) {

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
                .build();
    }
}
