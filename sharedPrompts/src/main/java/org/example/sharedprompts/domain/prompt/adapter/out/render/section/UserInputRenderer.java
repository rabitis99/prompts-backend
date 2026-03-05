package org.example.sharedprompts.domain.prompt.adapter.out.render.section;

import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

/**
 * [USER INPUT] 섹션 렌더러.
 */
public final class UserInputRenderer {

    private UserInputRenderer() {
    }

    public static String render(PromptSpec spec) {
        if (spec == null) {
            return "";
        }

        String clarified = spec.getClarifiedInput();
        String raw = spec.getRawInput();

        String content = (clarified != null && !clarified.isBlank())
                ? clarified
                : raw;

        if (content == null || content.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[USER INPUT]\n");
        sb.append("\"\"\"\n")
                .append(content)
                .append("\n\"\"\"\n\n");
        return sb.toString();
    }
}

