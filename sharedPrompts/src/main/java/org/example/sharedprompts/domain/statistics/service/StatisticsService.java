package org.example.sharedprompts.domain.statistics.service;

import org.example.sharedprompts.dto.statistics.response.AiCallStatisticsResponseDto;
import org.example.sharedprompts.dto.statistics.response.MyStatisticsResponseDto;
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

    /**
     * 캐시를 사용하지 않고 전체 통계 조회 (캐시 갱신용)
     * 데이터 검증 후 캐시 갱신 시 사용
     *
     * @return 통합 통계 응답 DTO
     */
    StatisticsResponseDto getAllStatisticsWithoutCache();

    /**
     * 내 통계 조회
     *
     * @param userId 사용자 ID
     * @return 개인 통계 응답 DTO
     */
    MyStatisticsResponseDto getMyStatistics(Long userId);
}
