package org.example.sharedprompts.dto.prompt.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.EtcActionType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.EtcRoleType;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeSerializer;
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

    @NotNull(message = "작업 유형을 입력해주세요.")
    @JsonProperty("action_type")
    @JsonSerialize(using = ActionTypeSerializer.class)
    @JsonDeserialize(using = ActionTypeDeserializer.class)
    private ActionTypeInterface actionType;
    
    @NotNull(message = "역할 유형을 입력해주세요.")
    @JsonProperty("role_type")
    @JsonSerialize(using = RoleTypeSerializer.class)
    @JsonDeserialize(using = RoleTypeDeserializer.class)
    private RoleTypeInterface roleType;

    private ToneType tone;
    private ExperienceLevel experience;
    private StyleType style;
    private LanguageType language;

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
            ActionTypeInterface actionType,
            RoleTypeInterface roleType,
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
                .actionType(actionType)
                .roleType(roleType)
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
        // @NotNull 검증이 적용되는 경우 actionType과 roleType은 null이 될 수 없지만,
        // 방어적 코딩을 위해 null 체크를 유지합니다 (직렬화/역직렬화 과정 등 검증이 우회될 수 있는 경로 대비)
        ActionTypeInterface actionType = this.actionType != null ? this.actionType : EtcActionType.GENERAL_CONSULTATION;
        RoleTypeInterface roleType = this.roleType != null ? this.roleType : EtcRoleType.GENERAL_CONSULTANT;

        return InputRequestDto.builder()
                .input(this.input)
                .actionType(actionType)
                .roleType(roleType)
                .tone(tone)
                .experience(level)
                .style(style)
                .language(language)
                .promptCategory(this.promptCategory)
                .tags(this.tags != null ? List.copyOf(this.tags) : null)
                .build();
    }
}
