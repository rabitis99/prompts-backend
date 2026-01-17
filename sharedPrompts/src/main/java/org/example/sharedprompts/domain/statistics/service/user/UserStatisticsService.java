package org.example.sharedprompts.domain.statistics.service.user;

import org.example.sharedprompts.dto.statistics.response.UserStatisticsResponseDto;

/**
 * 사용자 통계 서비스 인터페이스
 */
public interface UserStatisticsService {

    /**
     * 사용자 통계 조회
     *
     * @return 사용자 통계 응답 DTO
     */
    UserStatisticsResponseDto getUserStatistics();
}

