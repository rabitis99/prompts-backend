package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.repair.RepairPromptRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.ConstraintsSectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.ObjectiveSectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.OutputContractRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.RoleContextRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.StrategySectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.external.llm.rendering.section.UserInputRenderer;
import org.example.sharedprompts.domain.prompt.application.port.out.render.PromptSpecRendererPort;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.springframework.stereotype.Component;

/**
 * PromptSpec를 LLM 메타프롬프트로 변환하는 어댑터
 */
@Component
@RequiredArgsConstructor
public class PromptSpecRendererAdapter implements PromptSpecRendererPort {

    private final RepairPromptRenderer repairPromptRenderer;
    private final RoleContextRenderer roleContextRenderer;

    @Override
    public String render(PromptSpec spec) {

        // PromptSpec 섹션 조립
        return ObjectiveSectionRenderer.render(spec) +
                StrategySectionRenderer.render(spec) +
                ConstraintsSectionRenderer.render(spec) +
                roleContextRenderer.render(spec) +
                OutputContractRenderer.render(spec) +
                UserInputRenderer.render(spec);
    }

    @Override
    public String renderRepair(String draft, PromptSpec spec, String failureHints) {

        // Repair 프롬프트 생성 위임
        return repairPromptRenderer.renderRepair(draft, spec, failureHints);
    }
}