package org.example.sharedprompts.domain.prompt.adapter.out;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.application.port.out.PromptSpecRendererPort;
import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.springframework.stereotype.Component;

/**
 * PromptSpec → LLM 메타프롬프트 문자열 변환기.
 *
 * <p>기존 {@code PromptGenerator}의 역할을 헥사고날 구조로 재배치한 어댑터다.
 * 전략/섹션/가이드라인을 LLM이 이해할 수 있는 텍스트로 렌더링한다.
 * 수리용 프롬프트는 {@link RepairPromptRenderer}에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class PromptSpecRendererAdapter implements PromptSpecRendererPort {

    private final RepairPromptRenderer repairPromptRenderer;

    @Override
    public String render(PromptSpec spec) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert AI prompt engineer. Transform the user's input into a concise, high-quality prompt.\n\n");
        sb.append("## Objective\n")
          .append("This prompt is for: **").append(spec.getObjective().name()).append("** tasks.\n\n");
        sb.append(PromptSpecRenderHelper.renderStrategies(spec.getStrategyBundle().getStrategies()));
        sb.append("## Hard Rules\n")
          .append("- Output ONLY the core prompt body.\n")
          .append("- Do NOT include role assignment, tone/style labels, or explanations in the output.\n");
        if (spec.getConstraints().getMaxLength() != null) {
            sb.append("- Keep the output under ").append(spec.getConstraints().getMaxLength()).append(" characters.\n");
        }
        sb.append("\n");
        sb.append(PromptSpecRenderHelper.renderSections(spec.getSections()));
        if (spec.getOutputContract().hasJsonSchema()) {
            sb.append("## Output Format (Strict JSON Schema)\n")
              .append("```json\n").append(spec.getOutputContract().getJsonSchema()).append("\n```\n\n");
        }
        if (spec.getRole() != null) {
            sb.append("## Role Context\n")
              .append("The prompt is for an AI acting as **").append(spec.getRole().getRoleNameEn())
              .append("** (").append(spec.getRole().getDescriptionEn()).append(").\n\n");
        }
        sb.append("## Tone & Style\n")
          .append("- **Tone**: ").append(spec.getTone().getGuidelineEn()).append("\n")
          .append("- **Style**: ").append(spec.getStyle().getGuidelineEn()).append("\n\n");
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
        return repairPromptRenderer.renderRepair(draft, spec, failureHints);
    }
}
