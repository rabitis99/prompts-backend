package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section;

import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.common.PromptSpecRenderHelper;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

/**
 * [CONSTRAINTS] 섹션 렌더러.
 */
public final class ConstraintsSectionRenderer {

    private ConstraintsSectionRenderer() {
    }

    public static String render(PromptSpec spec) {
        if (spec == null || spec.getConstraints() == null) {
            return "";
        }

        String body = PromptSpecRenderHelper.renderConstraints(spec.getConstraints());
        if (body.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[CONSTRAINTS]\n");
        sb.append(body.trim()).append("\n\n");
        return sb.toString();
    }
}

