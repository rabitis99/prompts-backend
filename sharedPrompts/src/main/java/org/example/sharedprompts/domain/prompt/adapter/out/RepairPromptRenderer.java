package org.example.sharedprompts.domain.prompt.adapter.out;

import org.example.sharedprompts.domain.prompt.domain.model.PromptSpec;
import org.springframework.stereotype.Component;

/**
 * Repair 단계 전용 프롬프트 렌더러.
 * <p>실패 힌트·원본 초안·제약·전략·톤/스타일을 조합해 수리용 메타프롬프트를 만든다.</p>
 */
@Component
public class RepairPromptRenderer {

    /**
     * 수리용 메타프롬프트 문자열을 생성한다.
     *
     * @param draft         수정할 원본 초안
     * @param spec          동일 스펙(제약·전략·톤 유지)
     * @param failureHints  검증 실패 항목 힌트
     * @return LLM에 전달할 수리 지시 문자열
     */
    public String renderRepair(String draft, PromptSpec spec, String failureHints) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert AI prompt engineer. Repair the following prompt draft.\n\n");
        sb.append(failureHints).append("\n");
        sb.append("## Original Draft\n\"\"\"\n").append(draft).append("\n\"\"\"\n\n");
        sb.append("## Objective\n").append(spec.getObjective().name()).append("\n\n");
        sb.append(PromptSpecRenderHelper.renderStrategies(spec.getStrategyBundle().getStrategies()));
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
        sb.append("## Tone & Style\n")
          .append("- **Tone**: ").append(spec.getTone().getGuidelineEn()).append("\n")
          .append("- **Style**: ").append(spec.getStyle().getGuidelineEn()).append("\n\n");
        sb.append("Keep all correct parts unchanged. ")
          .append("Language: ").append(spec.getLocale().getDescription()).append("\n");
        return sb.toString();
    }
}
