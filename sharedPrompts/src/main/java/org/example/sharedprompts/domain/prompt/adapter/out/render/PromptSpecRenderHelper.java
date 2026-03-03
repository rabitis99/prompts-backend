package org.example.sharedprompts.domain.prompt.adapter.out.render;

import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSection;
import org.example.sharedprompts.domain.prompt.domain.value.strategy.PromptingStrategy;

import java.util.List;
import java.util.Set;

/**
 * PromptSpec 렌더링 시 전략·섹션 문단을 공통으로 생성하는 헬퍼.
 * {@link PromptSpecRendererAdapter}와 {@link RepairPromptRenderer}에서 공유한다.
 */
final class PromptSpecRenderHelper {

    private PromptSpecRenderHelper() {}

    static String renderStrategies(Set<PromptingStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("## Active Strategies\n");
        for (PromptingStrategy s : strategies) {
            sb.append("- **").append(s.name()).append("** [").append(s.getTier()).append("]\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    static String renderSections(List<PromptSection> sections) {
        if (sections == null || sections.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("## Sections to Include\n");
        for (PromptSection section : sections) {
            if (section.getContent() != null && !section.getContent().isBlank()) {
                sb.append("### ").append(section.getType().name()).append("\n")
                  .append(section.getContent()).append("\n\n");
            }
        }
        return sb.toString();
    }
}
