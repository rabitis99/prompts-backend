package org.example.sharedprompts.domain.prompt.adapter.out.render.section;

import org.example.sharedprompts.domain.prompt.common.enums.LanguageType;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

/**
 * [ROLE CONTEXT] 섹션 렌더러.
 */
public final class RoleContextRenderer {

    private RoleContextRenderer() {
    }

    public static String render(PromptSpec spec) {
        if (spec == null) {
            return "";
        }

        RoleTypeInterface role = spec.getRole();
        LanguageType locale = spec.getLocale();

        if (role == null) {
            // 역할이 없더라도 톤/스타일 정보는 제공한다.
            String toneGuideline = spec.getTone() != null
                    ? spec.getTone().getGuidelineByLang(locale)
                    : null;
            String styleGuideline = spec.getStyle() != null
                    ? spec.getStyle().getGuidelineByLang(locale)
                    : null;

            if ((toneGuideline == null || toneGuideline.isBlank())
                    && (styleGuideline == null || styleGuideline.isBlank())) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[ROLE CONTEXT]\n");
            if (toneGuideline != null && !toneGuideline.isBlank()) {
                sb.append("- Tone: ").append(toneGuideline).append("\n");
            }
            if (styleGuideline != null && !styleGuideline.isBlank()) {
                sb.append("- Style: ").append(styleGuideline).append("\n");
            }
            sb.append("\n");
            return sb.toString();
        }

        String roleName = role.getRoleNameByLang(locale);
        String roleDescription = role.getDescriptionByLang(locale);
        String toneGuideline = spec.getTone() != null
                ? spec.getTone().getGuidelineByLang(locale)
                : null;
        String styleGuideline = spec.getStyle() != null
                ? spec.getStyle().getGuidelineByLang(locale)
                : null;

        StringBuilder sb = new StringBuilder();
        sb.append("[ROLE CONTEXT]\n");
        sb.append("- Role: ").append(roleName);
        if (roleDescription != null && !roleDescription.isBlank()) {
            sb.append(" (").append(roleDescription).append(")");
        }
        sb.append("\n");

        if (toneGuideline != null && !toneGuideline.isBlank()) {
            sb.append("- Tone: ").append(toneGuideline).append("\n");
        }
        if (styleGuideline != null && !styleGuideline.isBlank()) {
            sb.append("- Style: ").append(styleGuideline).append("\n");
        }

        sb.append("\n");
        return sb.toString();
    }
}

