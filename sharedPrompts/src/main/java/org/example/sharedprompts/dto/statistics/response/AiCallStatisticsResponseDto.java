package org.example.sharedprompts.dto.statistics.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 호출 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCallStatisticsResponseDto {

    /**
     * 전체 호출 횟수
     */
    @JsonProperty("total_calls")
    private Long totalCalls;

    /**
     * 성공 호출 횟수
     */
    @JsonProperty("success_calls")
    private Long successCalls;

    /**
     * 실패 호출 횟수 (Fallback 사용)
     */
    @JsonProperty("failed_calls")
    private Long failedCalls;

    /**
     * 성공률 (%)
     */
    @JsonProperty("success_rate")
    private Double successRate;

    /**
     * 평균 응답 시간 (ms)
     */
    @JsonProperty("average_response_time_ms")
    private Double averageResponseTimeMs;

    /**
     * 최근 24시간 호출 횟수
     */
    @JsonProperty("last_24h_calls")
    private Long last24hCalls;

    /**
     * 최근 24시간 성공률 (%)
     */
    @JsonProperty("last_24h_success_rate")
    private Double last24hSuccessRate;
}
