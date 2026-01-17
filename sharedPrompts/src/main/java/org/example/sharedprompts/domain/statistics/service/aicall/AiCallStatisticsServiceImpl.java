package org.example.sharedprompts.domain.statistics.service.aicall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.statistics.util.StatisticsMathUtils;
import org.example.sharedprompts.dto.statistics.response.AiCallStatisticsResponseDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 호출 통계 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiCallStatisticsServiceImpl implements AiCallStatisticsService {

    private static final String CACHE_NAME = "statistics";

    private final AiCallMetricsCollector metricsCollector;

    @Override
    @Cacheable(value = CACHE_NAME, key = "'ai-call'", unless = "#result == null")
    public AiCallStatisticsResponseDto getAiCallStatistics() {
        log.debug("AI 호출 통계 조회");

        AiCallMetricsData metricsData = metricsCollector.collectMetrics();

        // 성공률 계산
        double successRate = StatisticsMathUtils.calculatePercentage(
                metricsData.getSuccessCalls(), metricsData.getTotalCalls());

        // 평균 응답 시간 계산
        double averageResponseTimeMs = calculateAverageResponseTime(
                metricsData.getTotalTime(), metricsData.getTotalTimeCount());

        // 최근 24시간 통계 (현재는 전체 통계를 사용, 향후 sliding window 구현 필요)
        double last24hSuccessRate = successRate;

        return AiCallStatisticsResponseDto.builder()
                .totalCalls(metricsData.getTotalCalls())
                .successCalls(metricsData.getSuccessCalls())
                .failedCalls(metricsData.getFailedCalls())
                .successRate(successRate)
                .averageResponseTimeMs(averageResponseTimeMs)
                .last24hCalls(metricsData.getTotalCalls())
                .last24hSuccessRate(last24hSuccessRate)
                .build();
    }

    /**
     * 평균 응답 시간 계산
     */
    private double calculateAverageResponseTime(double totalTime, long totalTimeCount) {
        if (totalTimeCount == 0) {
            return 0.0;
        }
        double average = totalTime / totalTimeCount;
        return StatisticsMathUtils.roundToTwoDecimals(average);
    }
}

