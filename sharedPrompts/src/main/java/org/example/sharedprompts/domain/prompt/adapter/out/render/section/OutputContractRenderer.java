package org.example.sharedprompts.domain.prompt.adapter.out.render.section;

import org.example.sharedprompts.domain.prompt.adapter.out.render.common.PromptSpecRenderHelper;
import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

/**
 * [OUTPUT FORMAT] 섹션 렌더러.
 */
public final class OutputContractRenderer {

    private OutputContractRenderer() {
    }

    public static String render(PromptSpec spec) {
        if (spec == null || spec.getOutputContract() == null) {
            return "";
        }

        OutputContract contract = spec.getOutputContract();

        StringBuilder sb = new StringBuilder();
        sb.append("[OUTPUT FORMAT]\n");
        sb.append("- Produce ONLY the final prompt text that should be sent to the target AI model.\n");
        sb.append("- Do not include explanations, meta commentary, section headers, or system messages in your answer.\n");
        sb.append("- Return exactly one prompt body. Do not append notes, disclaimers, or any extra text before or after it.\n");
        sb.append("- Expected format: ").append(contract.getFormat().name()).append("\n");
        if (contract.getMaxTokens() != null) {
            sb.append("- Max tokens: ").append(contract.getMaxTokens()).append("\n");
        }

        if (contract.hasJsonSchema()) {
            sb.append("- Strict JSON Schema (follow exactly):\n");
            sb.append("```json\n")
                    .append(contract.getJsonSchema())
                    .append("\n```\n");
        }

        String sectionsText = PromptSpecRenderHelper.renderSections(spec.getSections());
        if (!sectionsText.isBlank()) {
            sb.append("- Structure:\n");
            sb.append(sectionsText);
        }

        if (spec.getLocale() != null) {
            sb.append("- Language: ")
                    .append(spec.getLocale().getDescription())
                    .append(" (")
                    .append(spec.getLocale().getPromptToken())
                    .append(")\n");
        }

        sb.append("\n");
        return sb.toString();
    }
}

