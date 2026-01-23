package org.example.sharedprompts.dto.prompt.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;

import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PromptUpdateDto {

    @Size(max = 200, message = "제목은 최대 200자까지 입력해주세요.")
    private String title;

    @Size(max = 5000, message = "설명은 최대 5000자까지 입력해주세요.")
    private String description;

    @JsonProperty("is_public")
    private Boolean isPublic;

    @JsonProperty("prompt_category")
    private PromptCategory promptCategory;

    @Valid
    private List<@Size(min = 1, max = 50, message = "태그는 1~50자로 입력해주세요.") String> tags;

    public void applyTo(Prompt prompt) {
        if (title != null) {
            prompt.updateTitle(title);
        }
        if (description != null) {
            prompt.updateDescription(description);
        }
        if (isPublic != null) {
            prompt.updateIsPublic(isPublic);
        }
        if (promptCategory != null) {
            prompt.updateCategory(promptCategory);
        }

    }
}
