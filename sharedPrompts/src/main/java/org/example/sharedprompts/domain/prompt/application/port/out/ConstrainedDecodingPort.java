package org.example.sharedprompts.domain.prompt.application.port.out;

import org.example.sharedprompts.domain.prompt.domain.model.OutputContract;

/**
 * Constrained Decoding 포트 — JSON Schema 기반 토큰 마스킹으로 무효 출력을 원천 차단한다.
 *
 * <p>EXTRACTION Objective에서 우선 사용된다.
 * 벤더별 compliance rate 편차(최대 2배)가 있으므로 adapter 인터페이스 뒤에 격리한다.
 */
public interface ConstrainedDecodingPort {

    /**
     * Constrained Decoding으로 구조화된 출력을 생성한다.
     *
     * @param prompt         생성 요청 프롬프트
     * @param outputContract 출력 계약 (JSON Schema 포함)
     * @return Schema를 준수하는 출력 텍스트
     */
    String generateConstrained(String prompt, OutputContract outputContract);

    /**
     * 생성된 출력이 OutputContract의 스키마를 준수하는지 검증한다.
     *
     * @param output         검증할 출력 텍스트
     * @param outputContract 출력 계약
     * @return 준수 여부
     */
    boolean validate(String output, OutputContract outputContract);

    /**
     * 이 어댑터의 compliance rate를 반환한다 (지표 수집용).
     */
    double getComplianceRate();
}
