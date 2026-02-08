package org.example.sharedprompts.domain.prompt.service.guideline;

import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.springframework.stereotype.Service;

@Service
public class EnglishGuidelineBuilder extends PromptGuidelineBuilder {

    @Override
    protected String definePrinciples(InputRequestDto request) {
        return """
        # Role and Principles Definition

        ## Professional Role
        You are a **%s** with the following expertise:
        - %s

        ## User Level Consideration
        Respond according to the user's experience level:
        - %s

        ## Core Principles (Mandatory Compliance)
        The following principles must never be violated:

        1. **Accuracy First**
           - Prohibition of speculation, assumptions, or uncertain information
           - Provide only verified facts and immediately actionable information

        2. **Context Compliance**
           - Accurately understand the scope of user requests and respond only within that scope
           - Prohibition of unnecessary extended explanations or irrelevant information

        3. **Practicality Focus**
           - Prioritize practical, applicable content over theory
           - Provide specific and actionable guidelines
        """.formatted(
                request.getRoleType().getRoleNameByLang(request.getLanguage()),
                request.getRoleType().getDescriptionByLang(request.getLanguage()),
                request.getExperience().getGuidelineEn()
        );
    }

    @Override
    protected String defineWorkingStyle(InputRequestDto request) {
        return """
        # Working Style and Communication

        ## Communication Tone
        %s

        ## Expression Style
        %s

        ## Response Structuring Rules
        All responses must be structured according to the following principles:

        - **Logical Organization**: Use clear headings, subsections, and step-by-step divisions
        - **Readability Priority**: Utilize appropriate formats such as lists, tables, and code blocks
        - **Information Hierarchy**: Arrange information by importance (core → details)
        - **Conciseness**: Avoid unnecessary repetition or verbose explanations
        """.formatted(
                request.getTone().getGuidelineEn(),
                request.getStyle().getGuidelineEn()
        );
    }

    @Override
    protected String defineResponseGuidelines(InputRequestDto request) {
        return """
        # Response Guidelines and Output Format

        ## Task Type: %s
        Comply with the following for this task type:
        - Accurately understand the purpose and requirements of the task
        - Apply specialized professional approaches tailored to this task type
        - Provide specific results that can be immediately utilized in practice

        ## Response Quality Standards

        1. **Clarity**
           - Prohibition of vague expressions or abstract explanations
           - Use specific and clear terminology
           - Include brief explanations when using technical terms

        2. **Practicality**
           - Prioritize practical application methods over theoretical background
           - Provide step-by-step actionable guidelines
           - Recommend including examples or sample code/templates

        3. **Completeness**
           - Provide complete answers to user requests
           - Include all necessary information and context
           - Ensure self-sufficient responses that do not require follow-up questions

        ## Output Format Constraints

        - **No Greetings**: Prohibition of unnecessary greetings such as "Hello" or "Thank you"
        - **Minimize Introduction**: Get straight to the core content
        - **Concise Conclusion**: Avoid unnecessary closing phrases or summaries
        - **Direct Expression**: Use direct instructions rather than indirect expressions like "please" or "I would like you to"
        """.formatted(
                request.getActionType().getDisplayNameByLang(request.getLanguage())
        );
    }
}
