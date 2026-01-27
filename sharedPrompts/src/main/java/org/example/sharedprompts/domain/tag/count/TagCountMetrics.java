package org.example.sharedprompts.domain.tag.count;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Getter
public class TagCountMetrics {

    private final MeterRegistry meterRegistry;

    private Counter successCounter;
    private Counter decreaseCounter;
    private Counter increaseCounter;
    private Counter dlqCounter;

    private static final String METRIC_PREFIX = "tag_count_update";

    @PostConstruct
    public void initCounters() {
        this.successCounter = Counter.builder(METRIC_PREFIX + "_total")
                .tag("status", "success")
                .description("Total number of successful tag count updates")
                .register(meterRegistry);

        this.decreaseCounter = Counter.builder(METRIC_PREFIX + "_tags_total")
                .tag("operation", "decrease")
                .description("Total number of tags decreased")
                .register(meterRegistry);

        this.increaseCounter = Counter.builder(METRIC_PREFIX + "_tags_total")
                .tag("operation", "increase")
                .description("Total number of tags increased")
                .register(meterRegistry);

        this.dlqCounter = Counter.builder(METRIC_PREFIX + "_dlq_total")
                .description("Total number of events added to DLQ")
                .register(meterRegistry);
    }
}
