package org.example.sharedprompts.domain.prompt.adapter.out.llm.monitoring;

/**
 * Constrained Decoding compliance율을 추적하는 컴포넌트.
 */
public interface ComplianceTracker {

    /**
     * 단일 응답에 대한 준수 여부를 기록한다.
     */
    void record(boolean compliant);

    /**
     * 현재 추적 중인 compliance율을 반환한다.
     */
    double currentRate();
}

