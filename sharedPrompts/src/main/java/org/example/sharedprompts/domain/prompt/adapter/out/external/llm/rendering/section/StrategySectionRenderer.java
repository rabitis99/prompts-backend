package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section;

import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.common.PromptSpecRenderHelper;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

/**
 * [STRATEGIES] 섹션 렌더러.
 */
public final class StrategySectionRenderer {

    private StrategySectionRenderer() {
    }

    public static String render(PromptSpec spec) {
        if (spec == null || spec.getStrategyBundle() == null) {
            return "";
        }

        String body = PromptSpecRenderHelper.renderStrategies(spec.getStrategyBundle().getStrategies());
        if (body == null || body.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[STRATEGIES]\n");
        sb.append("Use these strategies internally to clarify ambiguous input, structure complex tasks, and validate the prompt before returning it. Do not mention strategy names in the final prompt body.\n");
        sb.append("Do not copy any text from this [STRATEGIES] section into the final prompt body; only apply it implicitly.\n");
        sb.append("Apply only the strategies that materially improve the prompt; do not force every strategy into the final design.\n");
        sb.append(body.trim()).append("\n\n");
        return sb.toString();
    }
}
