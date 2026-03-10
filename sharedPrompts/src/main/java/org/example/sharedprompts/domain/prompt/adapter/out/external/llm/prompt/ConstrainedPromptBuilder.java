package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.prompt;

import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;

/**
 * 제약 조건이 적용된 LLM 프롬프트 생성 인터페이스
 */
public interface ConstrainedPromptBuilder {

    /**
     * 출력 계약 기반 제약 프롬프트 생성
     */
    String build(String prompt, OutputContract contract);
}