package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.repair;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.ConstraintsSectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.ObjectiveSectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.OutputContractRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.RoleContextRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.StrategySectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.UserInputRenderer;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Repair 단계 프롬프트 렌더러
 */
@Component
@RequiredArgsConstructor
public class RepairPromptRenderer {

    private final RoleContextRenderer roleContextRenderer;

    /**
     * Repair 메타프롬프트 생성
     */
    public String renderRepair(String draft, PromptSpec spec, String failureHints) {

        StringBuilder sb = new StringBuilder();

        // Repair 작업 지시
        sb.append("You are an expert AI prompt engineer. Repair the following prompt draft so it satisfies the specification and constraints while changing as little as necessary.\n\n");

        // 실패 힌트 추가
        if (failureHints != null && !failureHints.isBlank()) {
            sb.append("[FAILURE HINTS]\n")
                    .append(failureHints.trim())
                    .append("\n\n");
        }

        // 원본 초안 포함 (동적 구분자로 감싸서 내용 변형 없이 전달, 구분자 충돌 방지)
        String safeDraft = draft != null ? draft : "";
        String draftDelimiter = "ORIGINAL_DRAFT_" + UUID.randomUUID();
        int attempts = 0;
        while (safeDraft.contains(draftDelimiter) && attempts++ < 5) {
            draftDelimiter = "ORIGINAL_DRAFT_" + UUID.randomUUID();
        }
        sb.append("[ORIGINAL DRAFT ").append(draftDelimiter).append("]\n")
                .append(safeDraft)
                .append("\n[/ORIGINAL DRAFT ").append(draftDelimiter).append("]\n\n");

        // 공통 섹션 렌더링
        sb.append(ObjectiveSectionRenderer.render(spec));
        sb.append(StrategySectionRenderer.render(spec));
        sb.append(ConstraintsSectionRenderer.render(spec));
        sb.append(roleContextRenderer.render(spec));
        sb.append(OutputContractRenderer.render(spec));
        sb.append(UserInputRenderer.render(spec));

        // Repair 검증 규칙
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