package org.example.sharedprompts.domain.statistics.service.aicall;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 호출 메트릭 수집기
 */
@Component
@RequiredArgsConstructor
public class AiCallMetricsCollector {

    private static final String METRIC_NAME = "ai.call";

    private final MeterRegistry meterRegistry;

    /**
     * 메트릭 데이터 수집
     *
     * @return 메트릭 수집 결과
     */
    public AiCallMetricsData collectMetrics() {
        List<Timer> allTimers = new java.util.ArrayList<>(
                meterRegistry.find(METRIC_NAME).timers());

        long totalCalls = 0;
        long successCalls = 0;
        long failedCalls = 0;
        double totalTime = 0.0;
        long totalTimeCount = 0;

        for (Timer timer : allTimers) {
            long count = (long) timer.count();
            totalCalls += count;

            String resultTag = timer.getId().getTag("result");
            if ("success".equals(resultTag)) {
                successCalls += count;
            } else if ("fallback".equals(resultTag)) {
                failedCalls += count;
            }

            double meanTime = timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
            if (meanTime > 0) {
                totalTime += meanTime * count;
                totalTimeCount += count;
            }
        }

        return AiCallMetricsData.builder()
                .totalCalls(totalCalls)
                .successCalls(successCalls)
                .failedCalls(failedCalls)
                .totalTime(totalTime)
                .totalTimeCount(totalTimeCount)
                .build();
    }
}

