package org.example.sharedprompts.domain.prompt.application.port.out.llm;

import org.example.sharedprompts.domain.prompt.domain.model.contract.OutputContract;

/**
 * Constrained Decoding 포트 — JSON Schema 기반 토큰 마스킹으로 무효 출력을 원천 차단한다.
 *
 * <p>EXTRACTION Objective에서 우선 사용된다.
 * 벤더별 compliance rate 편차(최대 2배)가 있으므로 adapter 인터페이스 뒤에 격리한다.
 */
public interface ConstrainedDecodingPort {

    /**
     * Constrained Decoding으로 구조화된 출력을 생성한다.
     */
    String generateConstrained(String prompt, OutputContract outputContract);

    /**
     * 생성된 출력이 OutputContract의 스키마를 준수하는지 검증한다.
     */
    boolean validate(String output, OutputContract outputContract);
}
