package org.example.sharedprompts.domain.prompt.adapter.out;

import org.example.sharedprompts.domain.prompt.application.port.out.PromptSpecRendererPort;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.example.sharedprompts.domain.prompt.domain.value.PromptingStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PromptSpec → LLM 메타프롬프트 문자열 변환기.
 *
 * <p>기존 {@code PromptGenerator}의 역할을 헥사고날 구조로 재배치한 어댑터다.
 * 전략/섹션/가이드라인을 LLM이 이해할 수 있는 텍스트로 렌더링한다.
 */
@Component
public class PromptSpecRendererAdapter implements PromptSpecRendererPort {

    @Override
    public String render(PromptSpec spec) {
        StringBuilder sb = new StringBuilder();

        // === SYSTEM ROLE ===
        sb.append("You are an expert AI prompt engineer. Transform the user's input into a concise, high-quality prompt.\n\n");

        // === OBJECTIVE & STRATEGY ===
        sb.append("## Objective\n")
          .append("This prompt is for: **").append(spec.getObjective().name()).append("** tasks.\n\n");

        // === ACTIVE STRATEGIES ===
        sb.append(renderStrategies(spec.getStrategyBundle().getStrategies()));

        // === HARD RULES ===
        sb.append("## Hard Rules\n")
          .append("- Output ONLY the core prompt body.\n")
          .append("- Do NOT include role assignment, tone/style labels, or explanations in the output.\n");
        if (spec.getConstraints().getMaxLength() != null) {
            sb.append("- Keep the output under ").append(spec.getConstraints().getMaxLength()).append(" characters.\n");
        }
        sb.append("\n");

        // === SECTIONS ===
        sb.append(renderSections(spec.getSections()));

        // === OUTPUT CONTRACT ===
        if (spec.getOutputContract().hasJsonSchema()) {
            sb.append("## Output Format (Strict JSON Schema)\n")
              .append("```json\n").append(spec.getOutputContract().getJsonSchema()).append("\n```\n\n");
        }

        // === ROLE & STYLE ===
        if (spec.getRole() != null) {
            sb.append("## Role Context\n")
              .append("The prompt is for an AI acting as **").append(spec.getRole().getRoleNameEn())
              .append("** (").append(spec.getRole().getDescriptionEn()).append(").\n\n");
        }

        sb.append("## Tone & Style\n")
          .append("- **Tone**: ").append(spec.getTone().getGuidelineEn()).append("\n")
          .append("- **Style**: ").append(spec.getStyle().getGuidelineEn()).append("\n\n");

        // === CORE INPUT ===
        sb.append("## User Input\n")
          .append("\"\"\"\n")
          .append(spec.getClarifiedInput())
          .append("\n\"\"\"\n\n");

        sb.append("## Language\n")
          .append("Generate the prompt body in ").append(spec.getLocale().getDescription())
          .append(" (").append(spec.getLocale().getPromptToken()).append(").\n\n");

        return sb.toString();
    }

    @Override
    public String renderRepair(String draft, PromptSpec spec, String failureHints) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert AI prompt engineer. Repair the following prompt draft.\n\n");

        // 실패 항목 힌트
        sb.append(failureHints).append("\n");

        // 원본 초안
        sb.append("## Original Draft\n\"\"\"\n").append(draft).append("\n\"\"\"\n\n");

        // Objective (수리 범위 명확화)
        sb.append("## Objective\n").append(spec.getObjective().name()).append("\n\n");

        // 전략 유지 (Solve와 동일한 전략으로 수리)
        sb.append(renderStrategies(spec.getStrategyBundle().getStrategies()));

        // 제약 조건 (수리 후에도 동일 제약 준수)
        sb.append("## Constraints\n")
          .append("- Output ONLY the core prompt body.\n");
        if (spec.getConstraints().getMaxLength() != null) {
            sb.append("- Keep output under ").append(spec.getConstraints().getMaxLength()).append(" characters.\n");
        }
        if (spec.getConstraints().isRequireStepByStep()) {
            sb.append("- Must include step-by-step reasoning.\n");
        }
        if (spec.getConstraints().isRequireCitations()) {
            sb.append("- Must include citations or uncertainty markers.\n");
        }
        sb.append("\n");

        // 톤/스타일 유지
        sb.append("## Tone & Style\n")
          .append("- **Tone**: ").append(spec.getTone().getGuidelineEn()).append("\n")
          .append("- **Style**: ").append(spec.getStyle().getGuidelineEn()).append("\n\n");

        sb.append("Keep all correct parts unchanged. ")
          .append("Language: ").append(spec.getLocale().getDescription()).append("\n");

        return sb.toString();
    }

    private String renderStrategies(Set<PromptingStrategy> strategies) {
        if (strategies.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("## Active Strategies\n");
        for (PromptingStrategy s : strategies) {
            sb.append("- **").append(s.name()).append("** [").append(s.getTier()).append("]\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private String renderSections(List<PromptSection> sections) {
        if (sections.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("## Sections to Include\n");
        for (PromptSection section : sections) {
            if (!section.getContent().isBlank()) {
                sb.append("### ").append(section.getType().name()).append("\n")
                  .append(section.getContent()).append("\n\n");
            }
        }
        return sb.toString();
    }
}
