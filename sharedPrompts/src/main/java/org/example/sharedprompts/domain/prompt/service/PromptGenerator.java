package org.example.sharedprompts.domain.prompt.service;

import org.example.sharedprompts.domain.prompt.enums.*;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptGenerator {

    private static final String PREFIX = "너는 유용한 AI 비서야. ";

    /**
     * 입력 DTO를 받아 AI에게 바로 넣기 좋은 프롬프트 생성
     */
    public String generatePrompt(InputRequestDto request) {
        if (request == null) {
            return PREFIX + "사용자가 제공한 정보가 없습니다. 가능한 최선의 답변을 제공해주세요.";
        }

        StringBuilder sb = new StringBuilder(PREFIX);

        appendUserInput(sb, request.getInput());
        appendExperience(sb, request.getExperience());
        appendLanguage(sb, request.getLanguage());
        appendTone(sb, request.getTone());
        appendStyle(sb, request.getStyle());
        appendCategory(sb, request.getPromptCategory());
        appendTags(sb, request.getTags());

        sb.append("위 정보를 바탕으로, 사용자가 이해하기 쉽고 친절하게 답변을 작성해주세요.");

        return sb.toString().trim();
    }

    private void appendUserInput(StringBuilder sb, String input) {
        if (input != null && !input.isBlank()) {
            sb.append("사용자가 요청한 내용은 \"").append(input.trim()).append("\" 입니다. ");
        }
    }

    private void appendExperience(StringBuilder sb, ExperienceLevel experience) {
        if (experience != null) {
            sb.append("이 사용자는 ").append(experience.getDescription()).append(" 수준의 경력을 가지고 있으며, ");
        }
    }

    private void appendLanguage(StringBuilder sb, LanguageType language) {
        if (language != null) {
            sb.append("주로 사용하는 언어는 ").append(language.getDescription()).append("입니다. ");
        }
    }

    private void appendTone(StringBuilder sb, ToneType tone) {
        if (tone != null) {
            sb.append("응답 시 ").append(tone.getDescription()).append(" 톤으로 자연스럽게 작성해주세요. ");
        }
    }

    private void appendStyle(StringBuilder sb, StyleType style) {
        if (style != null) {
            sb.append("문장의 스타일은 ").append(style.getDescription()).append(" 스타일을 선호합니다. ");
        }
    }

    private void appendCategory(StringBuilder sb, PromptCategory category) {
        if (category != null) {
            sb.append("프롬프트의 카테고리는 ").append(category.getDescription()).append("입니다. ");
        }
    }

    private void appendTags(StringBuilder sb, List<String> tags) {
        if (tags == null || tags.isEmpty()) return;

        // 1. 전처리: 공백 제거, 영어 대문자, null/빈 제거, 중복 제거
        List<String> processedTags = tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .map(tag -> tag.matches("^[a-zA-Z]+$") ? tag.toUpperCase() : tag)
                .distinct()
                .toList();

        if (!processedTags.isEmpty()) {
            String joinedTags = String.join(", ", processedTags);
            sb.append("관련된 태그로는 ").append(joinedTags).append("이 있습니다. ");
        }
    }
}

