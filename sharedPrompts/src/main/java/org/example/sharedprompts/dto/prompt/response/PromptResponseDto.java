package org.example.sharedprompts.dto.prompt.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.Tag.Tag;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.dto.user.response.UserResponseDto;

import java.util.ArrayList;
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


    public static PromptResponseDto from(Prompt prompt, List<Tag> tags) {

        if (tags == null) {
            tags = List.of();
        }

        List<String> tagNames = tags.stream()
                .map(Tag::getName)
                .toList();

        return PromptResponseDto.builder()
                .id(prompt.getId())
                .title(prompt.getTitle())
                .description(prompt.getDescription())
                .content(prompt.getContent())
                .isPublic(prompt.isPublic())
                .promptCategory(prompt.getPromptCategory())
                .tags(tagNames)
                .userResponseDto(UserResponseDto.from(prompt.getAuthor()))
                .build();
    }
}
