package org.example.sharedprompts.domain.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 알림 처리 메트릭 수집
 */
@Component
@RequiredArgsConstructor
public class NotificationMetrics {

    private static final String NOTIFICATION_PROCESSED = "notification.processed";
    private static final String NOTIFICATION_FAILED = "notification.failed";
    private static final String NOTIFICATION_PROCESSING_TIME = "notification.processing.time";
    private static final String NOTIFICATION_PUBLISHED = "notification.published";
    private static final String NOTIFICATION_SSE_SENT = "notification.sse.sent";
    private static final String NOTIFICATION_SSE_FAILED = "notification.sse.failed";

    private final MeterRegistry meterRegistry;

    /**
     * 알림 처리 성공 메트릭 기록
     */
    public void recordProcessed(String type) {
        Counter.builder(NOTIFICATION_PROCESSED)
                .tag("type", type)
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 알림 처리 실패 메트릭 기록
     */
    public void recordFailed(String type, String errorCode) {
        Counter.builder(NOTIFICATION_FAILED)
                .tag("type", type)
                .tag("error_code", errorCode)
                .register(meterRegistry)
                .increment();
    }

    /**
     * 알림 처리 시간 측정을 위한 Timer.Sample 반환
     */
    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * 알림 처리 시간 기록
     */
    public void recordProcessingTime(Timer.Sample sample, String type, String status) {
        sample.stop(Timer.builder(NOTIFICATION_PROCESSING_TIME)
                .tag("type", type)
                .tag("status", status)
                .register(meterRegistry));
    }

    /**
     * 알림 메시지 발행 메트릭 기록
     */
    public void recordPublished(String type) {
        Counter.builder(NOTIFICATION_PUBLISHED)
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }

    /**
     * SSE 알림 전송 성공 메트릭 기록
     */
    public void recordSseSent(String type) {
        Counter.builder(NOTIFICATION_SSE_SENT)
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }

    /**
     * SSE 알림 전송 실패 메트릭 기록
     */
    public void recordSseFailed(String type) {
        Counter.builder(NOTIFICATION_SSE_FAILED)
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }
}

