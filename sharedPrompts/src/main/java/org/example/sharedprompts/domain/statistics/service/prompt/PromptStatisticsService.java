package org.example.sharedprompts.domain.statistics.service.prompt;

import org.example.sharedprompts.dto.statistics.response.PromptStatisticsResponseDto;

/**
 * 프롬프트 통계 서비스 인터페이스
 */
public interface PromptStatisticsService {

    /**
     * 프롬프트 통계 조회
     *
     * @return 프롬프트 통계 응답 DTO
     */
    PromptStatisticsResponseDto getPromptStatistics();
}

