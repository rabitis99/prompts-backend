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
}

