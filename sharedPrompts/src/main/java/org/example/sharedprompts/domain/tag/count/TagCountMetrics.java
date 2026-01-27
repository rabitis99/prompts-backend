package org.example.sharedprompts.domain.tag.count;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 태그 카운트 업데이트 관련 메트릭 중앙 관리 컴포넌트
 * - 모든 메트릭을 사전 등록하여 일관성 있는 관리
 * - 인라인 Counter.builder().register() 호출 방지
 */
@Component
@RequiredArgsConstructor
@Getter
public class TagCountMetrics {

    private final MeterRegistry meterRegistry;

    // 성공/실패 메트릭
    private Counter successCounter;
    private Counter failureCounter;
    
    // 태그별 증가/감소 메트릭
    private Counter decreaseCounter;
    private Counter increaseCounter;
    
    // 재시도 및 DLQ 메트릭
    private Counter retryCounter;
    private Counter dlqCounter;
    
    // 처리 시간 메트릭
    private Timer durationTimer;

    private static final String METRIC_PREFIX = "tag_count_update";
    private static final Set<String> KNOWN_ERROR_TYPES = Set.of(
            "TagCountUpdateException", "RedisConnectionException", "JsonProcessingException"
    );

    @PostConstruct
    public void initCounters() {
        // 성공 메트릭
        this.successCounter = Counter.builder(METRIC_PREFIX + "_total")
                .tag("status", "success")
                .description("Total number of successful tag count updates")
                .register(meterRegistry);

        // 실패 메트릭 (error_type 태그는 동적으로 추가)
        this.failureCounter = Counter.builder(METRIC_PREFIX + "_total")
                .tag("status", "failure")
                .description("Total number of failed tag count updates")
                .register(meterRegistry);

        // 태그별 증가/감소 메트릭
        this.decreaseCounter = Counter.builder(METRIC_PREFIX + "_tags_total")
                .tag("operation", "decrease")
                .description("Total number of tags decreased")
                .register(meterRegistry);

        this.increaseCounter = Counter.builder(METRIC_PREFIX + "_tags_total")
                .tag("operation", "increase")
                .description("Total number of tags increased")
                .register(meterRegistry);

        // 재시도 메트릭 (retry_count 태그는 동적으로 추가)
        this.retryCounter = Counter.builder(METRIC_PREFIX + "_retry_total")
                .description("Total number of retry attempts")
                .register(meterRegistry);

        // DLQ 메트릭
        this.dlqCounter = Counter.builder(METRIC_PREFIX + "_dlq_total")
                .description("Total number of events added to DLQ")
                .register(meterRegistry);

        // 처리 시간 메트릭
        this.durationTimer = Timer.builder(METRIC_PREFIX + "_duration_seconds")
                .description("Tag count update processing duration")
                .register(meterRegistry);
    }

    /**
     * 실패 메트릭 기록 (error_type 태그 포함)
     */
    public void recordFailure(String errorType) {
        String normalizedType = KNOWN_ERROR_TYPES.contains(errorType) ? errorType : "unknown";
        Counter.builder(METRIC_PREFIX + "_total")
                .tag("status", "failure")
                .tag("error_type", normalizedType)
                .description("Total number of failed tag count updates")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 재시도 메트릭 기록 (retry_count 태그 포함)
     */
    public void recordRetry(int retryCount) {
        Counter.builder(METRIC_PREFIX + "_retry_total")
                .tag("retry_count", String.valueOf(retryCount))
                .description("Total number of retry attempts")
                .register(meterRegistry)
                .increment();
    }
}
