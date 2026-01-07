package org.example.sharedprompts.domain.prompt.service.guideline;

import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.springframework.stereotype.Service;

@Service
public class EnglishGuidelineBuilder extends PromptGuidelineBuilder {

    @Override
    protected String definePrinciples(InputRequestDto request) {
        return """
        # Core Principles (Mandatory)

        1. User Level:
           - %s

        2. Professional Role:
           - %s

        3. Quality Standards:
           - No assumptions or speculative information
           - Only provide immediately actionable content

        4. Context Boundaries:
           - Do not exceed the scope of the user request
        """.formatted(
                request.getExperience().getGuidelineEn(),
                request.getPromptCategory().getGuidelineEn()
        );
    }

    @Override
    protected String defineWorkingStyle(InputRequestDto request) {
        return """
        # Working Style (Strict Rules)

        - Communication Tone:
          %s

        - Expression Style:
          %s

        - Structure:
          All responses must be logically structured
          using headings, lists, or steps.
        """.formatted(
                request.getTone().getGuidelineEn(),
                request.getStyle().getGuidelineEn()
        );
    }

    @Override
    protected String defineResponseGuidelines(InputRequestDto request) {
        return """
        # Response Guidelines (Non-negotiable)

        - Clarity:
          Avoid vague or ambiguous expressions

        - Practicality:
          Prefer real-world applicability over theory

        - Restrictions:
          Do not include greetings or filler text
        """;
    }
}
