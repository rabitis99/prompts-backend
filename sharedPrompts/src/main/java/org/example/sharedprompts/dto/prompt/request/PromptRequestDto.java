package org.example.sharedprompts.dto.prompt.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.enums.*;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.dto.prompt.validator.ValidInputContent;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptRequestDto {

    // ------------------------------
    // DB 저장용 필드
    // ------------------------------
    @NotBlank(message = "제목은 비어 있을 수 없습니다.")
    private String title;

    @NotBlank(message = "설명은 비어 있을 수 없습니다.")
    private String description;

    private Boolean isPublic;

    @NotBlank(message = "카테고리는 비어 있을 수 없습니다.")
    private PromptCategory promptCategory;

    private List<String> tags;

    // ------------------------------
    // AI 입력용 필드
    // ------------------------------
    @NotBlank(message = "입력값은 비어 있을 수 없습니다.")
    @ValidInputContent
    private String input;                  // rough input

    private ToneType tone;                 // 톤
    private ExperienceLevel experience;    // 경력/난이도
    private StyleType style;               // 스타일
    private LanguageType language;         // 언어

    // ------------------------------
    // AI 생성 content 적용 후 엔티티 변환
    // ------------------------------
    public Prompt toEntity(User user, String aiGeneratedContent) {
        return Prompt.builder()
                .title(this.title)
                .description(this.description)
                .content(aiGeneratedContent)
                .isPublic(this.isPublic != null && this.isPublic)
                .promptCategory(this.promptCategory)
                .author(user)
                .build();
    }

    // ------------------------------
    // InputRequestDto 변환
    // ------------------------------
    public InputRequestDto toInputRequestDto() {
        ToneType tone = this.tone != null ? this.tone : ToneType.NEUTRAL;
        StyleType style = this.style != null ? this.style : StyleType.NARRATIVE;
        ExperienceLevel level = this.experience != null ? this.experience : ExperienceLevel.MID;
        LanguageType language = this.language != null ? this.language : LanguageType.KOREAN;

        return InputRequestDto.builder()
                .input(this.input)
                .tone(tone)
                .experience(level)
                .style(style)
                .language(language)
                .promptCategory(this.promptCategory)
                .tags(this.tags)
                .build();
    }
}
