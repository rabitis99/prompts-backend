package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * EMA 방식으로 compliance율을 추적하는 구현체
 */
@Slf4j
@Component
public class EmaComplianceTracker implements ComplianceTracker {

    // 임계값 위반 기준
    private static final double BREACH_THRESHOLD = 0.90;

    // 임계값 복구 기준
    private static final double RECOVERY_THRESHOLD = 0.92;

    private static final long MIN_SAMPLES_FOR_ALERT = 20;

    private double complianceRate = 1.0;
    private boolean thresholdBreached = false;
    private long sampleCount = 0;

    @Override
    public synchronized void record(boolean compliant) {
        sampleCount++;
        complianceRate = 0.95 * complianceRate + 0.05 * (compliant ? 1.0 : 0.0);

        if (sampleCount < MIN_SAMPLES_FOR_ALERT) {
            return;
        }

        if (complianceRate < BREACH_THRESHOLD && !thresholdBreached) {
            thresholdBreached = true;
            log.error("[ConstrainedDecoding] compliance rate 임계값 위반: rate={}. 대체 adapter 검토 필요",
                    complianceRate);
        } else if (complianceRate >= RECOVERY_THRESHOLD && thresholdBreached) {
            thresholdBreached = false;
        }
    }

    @Override
    public synchronized double currentRate() {
        return complianceRate;
    }

    /** 테스트 또는 운영 환경에서 추적기 상태를 초기화할 때 사용 */
    public synchronized void reset() {
        this.complianceRate = 1.0;
        this.thresholdBreached = false;
        this.sampleCount = 0;
    }
}