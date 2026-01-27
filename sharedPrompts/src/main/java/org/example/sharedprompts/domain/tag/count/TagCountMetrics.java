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
 * - 정적 태그를 가진 메트릭은 사전 등록하여 일관성 있는 관리
 * - 동적 태그(error_type, retry_count)가 필요한 메트릭은 recordFailure(), recordRetry()에서 생성
 */
@Component
@RequiredArgsConstructor
@Getter
public class TagCountMetrics {

    private final MeterRegistry meterRegistry;

    // 성공/실패 메트릭
    private Counter successCounter;
    
    // 태그별 증가/감소 메트릭
    private Counter decreaseCounter;
    private Counter increaseCounter;
    
    // 재시도 및 DLQ 메트릭
    private Counter dlqCounter;
    
    // 처리 시간 메트릭
    private Timer durationTimer;

    private static final String METRIC_PREFIX = "tag_count_update";
    private static final Set<String> KNOWN_ERROR_TYPES = Set.of(
            "TagCountUpdateException", "RedisConnectionException", "JsonProcessingException"
    );
    
    /**
     * 재시도 횟수를 버킷으로 변환하여 메트릭 카디널리티 폭발 방지
     * 
     * @param retryCount 원본 재시도 횟수
     * @return 버킷팅된 재시도 횟수 문자열
     */
    private static String bucketizeRetryCount(int retryCount) {
        if (retryCount <= 0) {
            return "0";
        } else if (retryCount == 1) {
            return "1";
        } else if (retryCount <= 3) {
            return "2-3";
        } else if (retryCount <= 5) {
            return "4-5";
        } else if (retryCount <= 10) {
            return "6-10";
        } else {
            return "11+";
        }
    }

    @PostConstruct
    public void initCounters() {
        // 성공 메트릭
        this.successCounter = Counter.builder(METRIC_PREFIX + "_total")
                .tag("status", "success")
                .description("Total number of successful tag count updates")
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
     * 재시도 메트릭 기록 (retry_count 태그 포함, 버킷팅 적용)
     * 
     * 카디널리티 폭발을 방지하기 위해 재시도 횟수를 버킷으로 변환합니다.
     * 버킷: "1", "2-3", "4-5", "6-10", "11+"
     */
    public void recordRetry(int retryCount) {
        String bucket = bucketizeRetryCount(retryCount);
        Counter.builder(METRIC_PREFIX + "_retry_total")
                .tag("retry_count", bucket)
                .description("Total number of retry attempts")
                .register(meterRegistry)
                .increment();
    }
}
