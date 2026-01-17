package org.example.sharedprompts.domain.statistics.service.aicall;

import org.example.sharedprompts.dto.statistics.response.AiCallStatisticsResponseDto;

/**
 * AI 호출 통계 서비스 인터페이스
 */
public interface AiCallStatisticsService {

    /**
     * AI 호출 통계 조회
     *
     * @return AI 호출 통계 응답 DTO
     */
    AiCallStatisticsResponseDto getAiCallStatistics();
}

