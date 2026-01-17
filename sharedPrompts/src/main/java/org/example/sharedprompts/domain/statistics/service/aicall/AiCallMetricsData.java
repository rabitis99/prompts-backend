package org.example.sharedprompts.domain.statistics.service.aicall;

import lombok.Builder;
import lombok.Getter;

/**
 * AI 호출 메트릭 데이터
 */
@Getter
@Builder
public class AiCallMetricsData {
    private final long totalCalls;
    private final long successCalls;
    private final long failedCalls;
    private final double totalTime;
    private final long totalTimeCount;
}

