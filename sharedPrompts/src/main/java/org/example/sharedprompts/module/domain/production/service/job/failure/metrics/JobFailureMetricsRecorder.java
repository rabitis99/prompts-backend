package org.example.sharedprompts.module.domain.production.service.job.failure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobFailureMetricsRecorder {
    
    private final MeterRegistry registry;
    
    public void recordFailure(String jobId, String commandType, String failureReason) {
        Counter.builder("job.failed")
                .tag("command_type", commandType)
                .tag("reason", failureReason)
                .register(registry)
                .increment();
    }
}

