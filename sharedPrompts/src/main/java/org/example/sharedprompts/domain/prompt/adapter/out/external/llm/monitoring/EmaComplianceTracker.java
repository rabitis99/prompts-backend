package org.example.sharedprompts.domain.prompt.adapter.out.external.llm.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    private final AtomicReference<Double> complianceRate = new AtomicReference<>(1.0);
    private final AtomicBoolean thresholdBreached = new AtomicBoolean(false);

    @Override
    public void record(boolean compliant) {

        // EMA 기반 준수율 업데이트
        double current = complianceRate.updateAndGet(
                rate -> 0.95 * rate + 0.05 * (compliant ? 1.0 : 0.0));

        // 임계값 하락 감지
        if (current < BREACH_THRESHOLD && thresholdBreached.compareAndSet(false, true)) {
            log.error("[ConstrainedDecoding] compliance rate 임계값 위반: rate={}. 대체 adapter 검토 필요",
                    current);
        }
        // 임계값 복구
        else if (current >= RECOVERY_THRESHOLD) {
            thresholdBreached.set(false);
        }
    }

    @Override
    public double currentRate() {
        return complianceRate.get();
    }
}