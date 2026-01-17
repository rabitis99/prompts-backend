package org.example.sharedprompts.dto.statistics.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 통합 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponseDto {

    /**
     * 사용자 통계
     */
    @JsonProperty("user_statistics")
    private UserStatisticsResponseDto userStatistics;

    /**
     * 프롬프트 통계
     */
    @JsonProperty("prompt_statistics")
    private PromptStatisticsResponseDto promptStatistics;

    /**
     * AI 호출 통계
     */
    @JsonProperty("ai_call_statistics")
    private AiCallStatisticsResponseDto aiCallStatistics;
}
