package org.example.sharedprompts.domain.prompt.adapter.out.llm.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 지수 이동 평균(EMA)으로 compliance rate를 추적하는 구현체.
 */
@Slf4j
@Component
public class EmaComplianceTracker implements ComplianceTracker {

    private final AtomicReference<Double> complianceRate = new AtomicReference<>(1.0);
    private final AtomicBoolean thresholdBreached = new AtomicBoolean(false);

    @Override
    public void record(boolean compliant) {
        double current = complianceRate.updateAndGet(
                rate -> 0.95 * rate + 0.05 * (compliant ? 1.0 : 0.0));
        if (current < 0.9 && thresholdBreached.compareAndSet(false, true)) {
            log.error("[ConstrainedDecoding] compliance rate 임계값 위반: rate={}. 대체 adapter 검토 필요",
                    current);
        } else if (current >= 0.9) {
            thresholdBreached.set(false);
        }
    }

    @Override
    public double currentRate() {
        return complianceRate.get();
    }
}

