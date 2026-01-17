package org.example.sharedprompts.domain.statistics.service;

import org.example.sharedprompts.dto.statistics.response.AiCallStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.PromptStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.StatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.UserStatisticsResponseDto;

/**
 * 통계 서비스 인터페이스
 */
public interface StatisticsService {

    /**
     * 전체 통계 조회
     *
     * @return 통합 통계 응답 DTO
     */
    StatisticsResponseDto getAllStatistics();

    /**
     * 사용자 통계 조회
     *
     * @return 사용자 통계 응답 DTO
     */
    UserStatisticsResponseDto getUserStatistics();

    /**
     * 프롬프트 통계 조회
     *
     * @return 프롬프트 통계 응답 DTO
     */
    PromptStatisticsResponseDto getPromptStatistics();

    /**
     * AI 호출 통계 조회
     *
     * @return AI 호출 통계 응답 DTO
     */
    AiCallStatisticsResponseDto getAiCallStatistics();
}
