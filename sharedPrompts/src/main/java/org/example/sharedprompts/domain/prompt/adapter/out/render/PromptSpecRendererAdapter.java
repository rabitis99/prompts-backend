package org.example.sharedprompts.domain.prompt.adapter.out.render;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.prompt.adapter.out.render.repair.RepairPromptRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.ConstraintsSectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.ObjectiveSectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.OutputContractRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.RoleContextRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.StrategySectionRenderer;
import org.example.sharedprompts.domain.prompt.adapter.out.render.section.UserInputRenderer;
import org.example.sharedprompts.domain.prompt.application.port.out.render.PromptSpecRendererPort;
import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;
import org.springframework.stereotype.Component;

/**
 * PromptSpec → LLM 메타프롬프트 문자열 변환기.
 */
@Component
@RequiredArgsConstructor
public class PromptSpecRendererAdapter implements PromptSpecRendererPort {

    private final RepairPromptRenderer repairPromptRenderer;

    @Override
    public String render(PromptSpec spec) {
        return ObjectiveSectionRenderer.render(spec) +
                StrategySectionRenderer.render(spec) +
                ConstraintsSectionRenderer.render(spec) +
                RoleContextRenderer.render(spec) +
                OutputContractRenderer.render(spec) +
                UserInputRenderer.render(spec);
    }

    @Override
    public String renderRepair(String draft, PromptSpec spec, String failureHints) {
        return repairPromptRenderer.renderRepair(draft, spec, failureHints);
    }
}
