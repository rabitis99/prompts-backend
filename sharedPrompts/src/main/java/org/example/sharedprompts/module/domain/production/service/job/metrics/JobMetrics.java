package org.example.sharedprompts.module.domain.production.service.job.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class JobMetrics {

    private final MeterRegistry registry;

    public void recordJobCompleted(String commandType, Duration duration) {
        Timer.builder("job.duration")
            .tag("command_type", commandType)
            .tag("status", "completed")
            .register(registry)
            .record(duration);
    }

    public void recordJobFailed(String commandType, String failureReason) {
        Counter.builder("job.failed")
            .tag("command_type", commandType)
            .tag("reason", failureReason)
            .register(registry)
            .increment();
    }

    public void recordJobStatus(String commandType, String status) {
        Counter.builder("job.status")
            .tag("command_type", commandType)
            .tag("status", status)
            .register(registry)
            .increment();
    }

    public void recordOptimisticLockRetry(int attempt) {
        Counter.builder("job.update.optimistic_lock.retry")
            .description("Optimistic Lock 재시도 횟수")
            .tag("attempt", String.valueOf(attempt))
            .register(registry)
            .increment();
    }

    public void recordOptimisticLockFailure() {
        Counter.builder("job.update.optimistic_lock.failure")
            .description("Optimistic Lock 재시도 실패 (최대 재시도 횟수 초과)")
            .register(registry)
            .increment();
    }

    public void recordTransactionTimeout(String operation) {
        Counter.builder("job.transaction.timeout")
            .description("트랜잭션 타임아웃 발생 횟수")
            .tag("operation", operation)
            .register(registry)
            .increment();
    }

    public Timer.Sample startUpdateTimer() {
        return Timer.start(registry);
    }

    public void recordUpdateDuration(Timer.Sample sample, String operation, boolean success) {
        sample.stop(Timer.builder("job.update.duration")
            .description("Job 업데이트 소요 시간")
            .tag("operation", operation)
            .tag("status", success ? "success" : "failure")
            .register(registry));
    }
}

