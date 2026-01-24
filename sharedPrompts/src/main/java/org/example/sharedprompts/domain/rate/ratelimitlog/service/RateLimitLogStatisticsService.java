package org.example.sharedprompts.domain.rate.ratelimitlog.service;

import org.example.sharedprompts.dto.admin.response.RateLimitLogStatisticsResponseDto;

import java.time.LocalDateTime;

/**
 * Rate Limit 로그 통계 서비스 인터페이스
 * 
 * Rate Limit 로그의 통계 정보를 제공합니다.
 */
public interface RateLimitLogStatisticsService {

    /**
     * Rate Limit 로그 통계를 조회합니다.
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 통계 정보
     */
    RateLimitLogStatisticsResponseDto getStatistics(LocalDateTime startDate, LocalDateTime endDate);
}





