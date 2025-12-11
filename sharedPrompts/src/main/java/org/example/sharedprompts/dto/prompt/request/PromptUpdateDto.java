package org.example.sharedprompts.dto.prompt.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.prompt.enums.PromptCategory;

import java.util.List;
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PromptUpdateDto {

    private String title;

    private String description;

    private Boolean isPublic;

    private PromptCategory promptCategory;

    private List<String> tags;

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
