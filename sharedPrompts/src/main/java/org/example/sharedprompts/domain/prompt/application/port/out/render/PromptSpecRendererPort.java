package org.example.sharedprompts.domain.prompt.application.port.out.render;

import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

/** PromptSpec → LLM용 문자열 렌더링 포트 */
public interface PromptSpecRendererPort {

    String render(PromptSpec spec);

    String renderRepair(String draft, PromptSpec spec, String failureHints);
}
