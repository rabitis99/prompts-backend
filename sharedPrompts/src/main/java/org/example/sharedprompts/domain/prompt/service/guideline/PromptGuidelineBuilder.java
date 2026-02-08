package org.example.sharedprompts.domain.prompt.service.guideline;

import org.example.sharedprompts.dto.prompt.request.InputRequestDto;

public abstract class PromptGuidelineBuilder {

    public final String build(String basePrompt, InputRequestDto request) {
        return definePrinciples(request)
                + "\n\n"
                + basePrompt
                + "\n\n"
                + defineWorkingStyle(request)
                + "\n\n"
                + defineResponseGuidelines(request);
    }

    protected abstract String definePrinciples(InputRequestDto request);
    protected abstract String defineWorkingStyle(InputRequestDto request);
    protected abstract String defineResponseGuidelines(InputRequestDto request);
}
