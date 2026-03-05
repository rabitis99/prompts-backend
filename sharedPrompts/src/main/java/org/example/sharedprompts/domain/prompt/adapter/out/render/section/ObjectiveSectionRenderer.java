package org.example.sharedprompts.domain.prompt.adapter.out.render.section;

import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

/**
 * [OBJECTIVE] 섹션 렌더러.
 */
public final class ObjectiveSectionRenderer {

    private ObjectiveSectionRenderer() {
    }

    public static String render(PromptSpec spec) {
        if (spec == null || spec.getObjective() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert AI prompt engineer writing a prompt template for another AI assistant.\n");
        sb.append("Your goal is to transform the user's request into a clear, structured, and effective prompt.\n\n");

        sb.append("[OBJECTIVE]\n");
        sb.append("- Accurately capture and restate the user's intent.\n");
        sb.append("- Produce a concise prompt that is easy for the target AI to follow.\n");
        sb.append("- Target task type: ").append(spec.getObjective().name()).append(".\n");
        sb.append("- A high-quality prompt should be well structured, avoid unnecessary verbosity, and guide the AI toward useful and accurate responses.\n\n");
        return sb.toString();
    }
}
