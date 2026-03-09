package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.monitoring;

/**
 * Constrained Decoding 준수율 추적 인터페이스
 */
public interface ComplianceTracker {

    /**
     * 응답의 스키마 준수 여부 기록
     */
    void record(boolean compliant);

    /**
     * 현재 준수율 반환
     */
    double currentRate();
}