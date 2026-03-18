package org.example.sharedprompts.domain.prompt.application.semantic.observability;

import org.example.sharedprompts.domain.prompt.domain.semantic.observability.RecommendationMetricsEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory sink for tests and optional dev dashboards.
 * Thread-safe; events are appended and can be read/cleared for assertions.
 */
public class InMemoryRecommendationMetricsSink implements RecommendationMetricsSink {

    private final List<RecommendationMetricsEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void emit(RecommendationMetricsEvent event) {
        if (event != null) {
            events.add(event);
        }
    }

    public List<RecommendationMetricsEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public void clear() {
        events.clear();
    }

    public int size() {
        return events.size();
    }
}
