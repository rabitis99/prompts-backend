package org.example.sharedprompts.global.google.gemini.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.global.google.gemini.GoogleGeminiProperties;
import org.springframework.stereotype.Component;

/**
 * Gemini API 호출 메트릭을 기록하는 클래스
 */
@Component
@RequiredArgsConstructor
public class GeminiMetricsRecorder {

    private static final String METRIC_NAME = "ai.call";

    private final MeterRegistry meterRegistry;
    private final GoogleGeminiProperties properties;

    /**
     * API 호출 결과를 메트릭으로 기록합니다.
     *
     * @param sample 타이머 샘플
     * @param result 결과 상태 (success, fallback 등)
     * @param errorType 에러 타입 (에러인 경우)
     */
    public void recordMetrics(Timer.Sample sample, String result, String errorType) {
        Timer.Builder timerBuilder = Timer.builder(METRIC_NAME)
                .tag("provider", "gemini")
                .tag("model", properties.getModel())
                .tag("result", result);

        if (errorType != null) {
            timerBuilder.tag("error.type", errorType);
        }

        sample.stop(timerBuilder.register(meterRegistry));
    }
}

