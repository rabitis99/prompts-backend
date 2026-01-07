package org.example.sharedprompts.dto.prompt.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputRequestDto {

    private String input;
    private ToneType tone;
    private ExperienceLevel experience;
    private StyleType style;
    private LanguageType language;
    private PromptCategory promptCategory;
    private List<String> tags;
}
