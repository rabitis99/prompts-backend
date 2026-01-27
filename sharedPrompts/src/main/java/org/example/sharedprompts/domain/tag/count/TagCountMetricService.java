package org.example.sharedprompts.domain.tag.count;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 태그 카운트 업데이트 메트릭 서비스
 * - Prometheus 메트릭 등록
 * - 성공/실패/재시도/처리시간 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagCountMetricService {

    private final TagCountMetrics metrics;
    private final MeterRegistry meterRegistry;

    /**
     * 전체 성공 메트릭 증가
     */
    public void recordSuccess(int decreaseCount, int increaseCount) {
        metrics.getSuccessCounter().increment();
        metrics.getDecreaseCounter().increment(decreaseCount);
        metrics.getIncreaseCounter().increment(increaseCount);
    }

    /**
     * 태그별 성공 메트릭 기록
     * 
     * Note: tag_name 레이블은 높은 카디널리티 문제를 유발할 수 있어 제거했습니다.
     * 태그별 메트릭이 필요한 경우, 상위 N개 태그로 제한하거나 다른 방식으로 집계하세요.
     */
    public void recordTagSuccess(String operation) {
        // 높은 카디널리티 문제를 방지하기 위해 tag_name 레이블 제거
        Counter.builder("tag_count_update_tags_total")
                .tag("operation", operation)
                .description("Total number of tags updated by operation type")
                .register(meterRegistry)
                .increment();
    }

    public void recordFailure(String errorType) {
        Counter.builder("tag_count_update_total")
                .tag("status", "failure")
                .tag("error_type", errorType)
                .description("Total number of failed tag count updates")
                .register(meterRegistry)
                .increment();
    }

    public void recordRetry(int retryCount) {
        Counter.builder("tag_count_update_retry_total")
                .tag("retry_count", String.valueOf(retryCount))
                .description("Total number of retry attempts")
                .register(meterRegistry)
                .increment();
    }

    public void recordDlq() {
        metrics.getDlqCounter().increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDuration(Timer.Sample sample) {
        sample.stop(Timer.builder("tag_count_update_duration_seconds")
                .description("Tag count update processing duration")
                .register(meterRegistry));
    }
}

