package org.example.sharedprompts.domain.rate.ratelimitlog.service;

import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.example.sharedprompts.dto.admin.response.RateLimitLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Rate Limit 로그 서비스 인터페이스
 * Rate Limit 로그 조회 기능을 제공합니다.
 */
public interface RateLimitLogService {

    /**
     * 조건에 따라 Rate Limit 로그를 조회합니다.
     * 
     * @param userId 사용자 ID (null 가능)
     * @param clientIp 클라이언트 IP (null 가능)
     * @param ruleName 규칙 이름 (null 가능)
     * @param rateLimitType Rate Limit 타입 (null 가능)
     * @param startDate 시작 날짜 (null 가능, 시스템 기본 타임존 기준으로 해석)
     * @param endDate 종료 날짜 (null 가능, 포함 여부/배제 여부를 명시)
     * @param pageable 페이지네이션 정보
     * @return Rate Limit 로그 페이지
     */
    Page<RateLimitLogResponseDto> getRateLimitLogs(
            Long userId,
            String clientIp,
            String ruleName,
            RateLimitType rateLimitType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}
