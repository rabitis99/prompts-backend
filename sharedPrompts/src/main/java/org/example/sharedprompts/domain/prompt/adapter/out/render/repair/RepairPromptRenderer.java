package org.example.sharedprompts.domain.prompt.adapter.out.render.repair;

import org.example.sharedprompts.domain.prompt.adapter.out.render.section.ConstraintsSectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.ObjectiveSectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.OutputContractRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.RoleContextRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.StrategySectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.UserInputRenderer;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.springframework.stereotype.Component;

/**
 * Repair 단계 전용 프롬프트 렌더러.
 */
@Component
public class RepairPromptRenderer {

    /**
     * 수리용 메타프롬프트 문자열을 생성한다.
     */
    public String renderRepair(String draft, PromptSpec spec, String failureHints) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert AI prompt engineer. Repair the following prompt draft so it satisfies the specification and constraints while changing as little as necessary.\n\n");

        if (failureHints != null && !failureHints.isBlank()) {
            sb.append("[FAILURE HINTS]\n")
                    .append(failureHints.trim())
                    .append("\n\n");
        }

        String safeDraft = draft != null ? draft : "";
        String escapedDraft = safeDraft.replace("\"\"\"", "\\\"\\\"\\\"");
        sb.append("[ORIGINAL DRAFT]\n\"\"\"\n")
                .append(escapedDraft)
                .append("\n\"\"\"\n\n");

        // 공통 섹션 렌더러 재사용
        sb.append(ObjectiveSectionRenderer.render(spec));
        sb.append(StrategySectionRenderer.render(spec));
        sb.append(ConstraintsSectionRenderer.render(spec));
        sb.append(RoleContextRenderer.render(spec));
        sb.append(OutputContractRenderer.render(spec));
        sb.append(UserInputRenderer.render(spec));

        // Repair 단계 전용 엄격 검증 알림
        sb.append("[VALIDATION]\n");
        sb.append("- Ensure the final answer strictly satisfies all constraints and the output format.\n");
        sb.append("- Preserve all correct and constraint-satisfying parts of the original draft verbatim whenever possible.\n");
        sb.append("- Modify only the parts that are necessary to satisfy the constraints, failure hints, and output format; avoid rewriting the entire draft.\n");
        sb.append("- If failure hints are empty, apply only the minimal modifications required by the constraints or output format.\n");
        sb.append("- If failure hints are provided, fix those failures with the smallest possible edits while preserving already-correct parts.\n");
        sb.append("- Do not introduce new assumptions that are not supported by the specification or [USER INPUT].\n");
        return sb.toString();
    }
}
