package org.example.sharedprompts.dto.prompt.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;
import org.example.sharedprompts.dto.user.response.UserResponseDto;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResponseDto {

    private Long id;
    private String title;
    private String description;
    private String content; // AI가 생성한 최종 내용
    private boolean isPublic;
    private PromptCategory promptCategory;
    private UserResponseDto userResponseDto;

    /**
     * Prompt 엔티티를 DTO로 변환
     */
    public static PromptResponseDto from(Prompt prompt) {
        return PromptResponseDto.builder()
                .id(prompt.getId())
                .title(prompt.getTitle())
                .description(prompt.getDescription())
                .content(prompt.getContent())
                .isPublic(prompt.isPublic())
                .promptCategory(prompt.getPromptCategory())
                .userResponseDto(UserResponseDto.from(prompt.getAuthor()))
                .build();
    }
}
