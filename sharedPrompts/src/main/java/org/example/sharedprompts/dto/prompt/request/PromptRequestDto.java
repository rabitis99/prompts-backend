package org.example.sharedprompts.dto.prompt.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PromptRequestDto {

    // ------------------------------
    // DB 저장용 필드
    // ------------------------------
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 최대 200자까지 입력해주세요.")
    private String title;

    @NotBlank(message = "설명을 입력해주세요.")
    @Size(max = 5000, message = "설명은 최대 5000자까지 입력해주세요.")
    private String description;

    @JsonProperty("is_public")
    private Boolean isPublic;

    @NotNull(message = "카테고리를 입력해주세요.")
    @JsonProperty("prompt_category")
    private PromptCategory promptCategory;

    @Valid
    private List<@Size(min = 1, max = 50, message = "태그는 1~50자로 입력해주세요.") String> tags;

    // ------------------------------
    // AI 입력용 필드
    // ------------------------------
    @NotBlank(message = "입력값을 입력해주세요.")
    @ValidInputContent
    private String input;                  // rough input

    private ToneType tone;                 // 톤
    private ExperienceLevel experience;    // 경력/난이도
    private StyleType style;               // 스타일
    private LanguageType language;         // 언어

    /**
     * 방어적 복사를 적용한 정적 팩토리 메서드.
     * 외부에서 DTO를 생성할 때 tags 리스트가 변경되더라도 내부 상태는 유지된다.
     */
    public static PromptRequestDto of(
            String title,
            String description,
            Boolean isPublic,
            PromptCategory promptCategory,
            List<String> tags,
            String input,
            ToneType tone,
            ExperienceLevel experience,
            StyleType style,
            LanguageType language
    ) {
        return PromptRequestDto.builder()
                .title(title)
                .description(description)
                .isPublic(isPublic)
                .promptCategory(promptCategory)
                .tags(tags != null ? List.copyOf(tags) : null)
                .input(input)
                .tone(tone)
                .experience(experience)
                .style(style)
                .language(language)
                .build();
    }

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
        ExperienceLevel level = this.experience != null ? this.experience : ExperienceLevel.INTERMEDIATE;
        LanguageType language = this.language != null ? this.language : LanguageType.KOREAN;

        return InputRequestDto.builder()
                .input(this.input)
                .tone(tone)
                .experience(level)
                .style(style)
                .language(language)
                .promptCategory(this.promptCategory)
                .tags(this.tags != null ? List.copyOf(this.tags) : null)
                .build();
    }
}
