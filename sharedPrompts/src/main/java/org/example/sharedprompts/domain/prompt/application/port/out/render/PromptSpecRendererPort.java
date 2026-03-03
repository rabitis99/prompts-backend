package org.example.sharedprompts.domain.prompt.application.port.out.render;

import org.example.sharedprompts.domain.prompt.domain.model.spec.PromptSpec;

/**
 * PromptSpec → LLM 프롬프트 문자열 변환 포트.
 * 전략/섹션/가이드라인을 LLM이 이해할 수 있는 텍스트로 렌더링한다.
 */
public interface PromptSpecRendererPort {

    /**
     * PromptSpec을 LLM에 전달할 메타프롬프트 문자열로 변환한다.
     */
    String render(PromptSpec spec);

    /**
     * Repair 요청용 프롬프트를 생성한다.
     */
    String renderRepair(String draft, PromptSpec spec, String failureHints);
}
