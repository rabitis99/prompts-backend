package org.example.sharedprompts.domain.prompt.service;

import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.example.sharedprompts.global.util.TagNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptGenerator {

    public String generatePrompt(InputRequestDto request) {
        return buildMetaPrompt(request);
    }

    private String buildMetaPrompt(InputRequestDto request) {
        StringBuilder metaPrompt = new StringBuilder();

        // === SYSTEM ROLE ===
        metaPrompt.append("You are a senior AI prompt engineering expert.\n");
        metaPrompt.append("Your task is to rewrite and enhance a user request ");
        metaPrompt.append("into a clear, domain-aligned, high-quality AI prompt.\n\n");

        metaPrompt.append("Rules:\n");
        metaPrompt.append("- Do not explain your reasoning.\n");
        metaPrompt.append("- Do not include greetings or meta commentary.\n");
        metaPrompt.append("- Output only the final improved prompt.\n\n");

        // === CORE TASK ===
        metaPrompt.append(buildEnhancedRequest(request));

        return metaPrompt.toString();
    }

    /**
     * 핵심 변경 지점
     * - input을 그대로 쓰지 않는다
     * - category + tags를 이용해 의미를 재구성한다
     */
    private String buildEnhancedRequest(InputRequestDto request) {
        StringBuilder section = new StringBuilder();

        section.append("# Request Context\n\n");

        // 1. Domain framing
        section.append("This request is related to the following domain:\n");
        section.append("- Domain: ")
                .append(request.getPromptCategory().getDisplayName())
                .append("\n");
        section.append("- Domain Focus: ")
                .append(request.getPromptCategory().getGuidelineEn())
                .append("\n\n");

        // 2. User intent (rewritten)
        section.append("Based on this domain, enhance the following user intent ");
        section.append("into a clear and professional AI request:\n\n");
        section.append("Original Intent:\n");
        section.append("- ").append(request.getInput()).append("\n\n");

        // 3. Tag-based constraints
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            section.append("Relevant contextual keywords that must be reflected ");
            section.append("in the enhanced request:\n");
            section.append(formatTagsAsConstraints(request.getTags())).append("\n\n");
        }

        section.append("Rewrite the intent so that:\n");
        section.append("- The purpose is explicit and unambiguous\n");
        section.append("- The request aligns with the stated domain focus\n");
        section.append("- The result can be directly used as an AI instruction\n");

        return section.toString();
    }

    private String formatTagsAsConstraints(List<String> tags) {
        return TagNormalizer.normalizeTags(tags).stream()
                .map(tag -> "- Consider aspects related to: " + tag)
                .collect(Collectors.joining("\n"));
    }
}



