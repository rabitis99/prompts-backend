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
        metaPrompt.append("You are an expert AI prompt engineer. Rewrite the user's input into a concise, high-quality, actionable prompt.\n\n");

        // === HARD RULES ===
        metaPrompt.append("## Hard Rules\n")
                .append("- Output ONLY the final prompt text (no explanations, greetings, or prefixes).\n")
                .append("- Break long thoughts into 2-4 clear sentences.\n")
                .append("- Each sentence must focus on one main point.\n")
                .append("- Simple requests: 2-3 sentences. Complex requests: 3-5 sentences max.\n\n");

        // === CORE TASK ===
        metaPrompt.append(buildEnhancedRequest(request));

        return metaPrompt.toString();
    }

    /**
     * 핵심 변경 지점
     * - input을 그대로 쓰지 않는다
     * - category + tags를 이용해 의미를 재구성한다
     * - 다양한 관점과 접근 방식을 유도한다
     */
    private String buildEnhancedRequest(InputRequestDto request) {
        StringBuilder section = new StringBuilder();

        // 1. User Input (PRIMARY)
        section.append("## User Input\n")
                .append("\"\"\"\n")
                .append(request.getInput())
                .append("\n\"\"\"\n\n")
                .append("**Task**: Rewrite this into a clear, actionable prompt without changing the topic or intent.\n\n");

        // 2. Domain Context
        section.append("## Domain Context\n")
                .append("**Category**: ").append(request.getPromptCategory().getDisplayName()).append("\n")
                .append("**Guideline**: ").append(request.getPromptCategory().getGuidelineEn()).append("\n\n");

        // 3. Tags (if exist)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            section.append("## Key Themes\n")
                    .append(formatTagsAsConstraints(request.getTags()))
                    .append("\n\n");
        }

        // 4. User Preferences
        if (request.getExperience() != null || request.getTone() != null || request.getStyle() != null) {
            section.append("## Preferences\n");
            if (request.getExperience() != null) {
                section.append("**Level**: ").append(request.getExperience().getGuidelineEn()).append("\n");
            }
            if (request.getTone() != null) {
                section.append("**Tone**: ").append(request.getTone().getGuidelineEn()).append("\n");
            }
            if (request.getStyle() != null) {
                section.append("**Style**: ").append(request.getStyle().getGuidelineEn()).append("\n");
            }
            section.append("\n");
        }

        // 5. Output Language
        if (request.getLanguage() != null) {
            section.append("## Language\n")
                    .append("Generate the prompt in ")
                    .append(request.getLanguage().getDescription())
                    .append(" (")
                    .append(request.getLanguage().getPromptToken())
                    .append(").\n\n");
        }

        // 6. Key Requirements (중복 제거: Hard Rules와 역할 분리)
        section.append("## Requirements\n")
                .append("- Make it immediately actionable and model-agnostic\n")
                .append("- Use natural, original phrasing (avoid generic templates)\n");

        return section.toString();
    }


    private String formatTagsAsConstraints(List<String> tags) {
        return TagNormalizer.normalizeTags(tags).stream()
                .map(tag -> "- Consider aspects related to: " + tag)
                .collect(Collectors.joining("\n"));
    }
}



