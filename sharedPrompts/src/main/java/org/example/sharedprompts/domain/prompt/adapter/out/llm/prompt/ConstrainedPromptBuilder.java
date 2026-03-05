package org.example.sharedprompts.domain.prompt.adapter.out.llm.prompt;

import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;

/**
 * LLM에 전달할 constrained prompt를 구성하는 역할.
 */
public interface ConstrainedPromptBuilder {

    /**
     * 주어진 원본 프롬프트와 출력 계약을 기반으로, 제약 조건이 적용된 프롬프트를 생성한다.
     */
    String build(String prompt, OutputContract contract);
}

