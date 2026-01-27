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
 * - 성공/실패/재시도 횟수 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagCountMetricService {

    private final MeterRegistry meterRegistry;

    private static final String METRIC_PREFIX = "tag_count_update";

    /**
     * 성공 메트릭 증가
     */
    public void recordSuccess(int decreaseCount, int increaseCount) {
        Counter.builder(METRIC_PREFIX + "_total")
                .tag("status", "success")
                .description("Total number of successful tag count updates")
                .register(meterRegistry)
                .increment();
        
        Counter.builder(METRIC_PREFIX + "_tags_total")
                .tag("operation", "decrease")
                .description("Total number of tags decreased")
                .register(meterRegistry)
                .increment(decreaseCount);
        
        Counter.builder(METRIC_PREFIX + "_tags_total")
                .tag("operation", "increase")
                .description("Total number of tags increased")
                .register(meterRegistry)
                .increment(increaseCount);
    }

    /**
     * 태그별 성공 메트릭 증가 (tag_name 레이블 포함)
     * 
     * @param tagName 태그 이름
     * @param operation 작업 유형 ("increase" 또는 "decrease")
     */
    public void recordTagSuccess(String tagName, String operation) {
        Counter.builder(METRIC_PREFIX + "_tags_total")
                .tag("operation", operation)
                .tag("tag_name", tagName)
                .description("Total number of tags updated by name")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 실패 메트릭 증가
     */
    public void recordFailure(String errorType) {
        Counter.builder(METRIC_PREFIX + "_total")
                .tag("status", "failure")
                .tag("error_type", errorType)
                .description("Total number of failed tag count updates")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 재시도 메트릭 증가
     */
    public void recordRetry(int retryCount) {
        Counter.builder(METRIC_PREFIX + "_retry_total")
                .tag("retry_count", String.valueOf(retryCount))
                .description("Total number of retry attempts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * DLQ 추가 메트릭 증가
     */
    public void recordDlq() {
        Counter.builder(METRIC_PREFIX + "_dlq_total")
                .description("Total number of events added to DLQ")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 처리 시간 기록
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 처리 시간 기록 완료
     */
    public void recordDuration(Timer.Sample sample) {
        sample.stop(Timer.builder(METRIC_PREFIX + "_duration_seconds")
                .description("Tag count update processing duration")
                .register(meterRegistry));
    }
}

