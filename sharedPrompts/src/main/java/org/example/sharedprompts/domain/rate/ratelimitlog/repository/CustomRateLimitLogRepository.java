package org.example.sharedprompts.domain.rate.ratelimitlog.repository;

import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Rate Limit 로그 커스텀 Repository 인터페이스
 * 
 * 복잡한 쿼리 및 통계 조회를 위한 메서드를 정의합니다.
 * JpaRepository의 기본 메서드로 처리하기 어려운 복잡한 쿼리를 담당합니다.
 */
public interface CustomRateLimitLogRepository {

    /**
     * 복합 조건으로 조회
     * 
     * @param userId 사용자 ID (null 가능)
     * @param clientIp 클라이언트 IP (null 가능)
     * @param ruleName 규칙 이름 (null 가능)
     * @param rateLimitType Rate Limit 타입 (null 가능)
     * @param startDate 시작 날짜 (null 가능)
     * @param endDate 종료 날짜 (null 가능)
     * @param pageable 페이지네이션 정보
     * @return RateLimitLog 페이지
     */
    Page<RateLimitLog> findByConditions(
            Long userId,
            String clientIp,
            String ruleName,
            RateLimitType rateLimitType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 오래된 로그 삭제
     * 
     * @param cutoffDate 삭제 기준 날짜 (이전 날짜의 로그 삭제)
     * @return 삭제된 로그 수
     */
    int deleteOldLogs(LocalDateTime cutoffDate);

    /**
     * 시간대별 통계 조회
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 시간대별 로그 수 (Map<시간, 개수>)
     */
    Map<Integer, Long> countByHour(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 규칙별 통계 조회
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 규칙별 로그 수 (Map<규칙명, 개수>)
     */
    Map<String, Long> countByRuleName(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 타입별 통계 조회
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 타입별 로그 수 (Map<타입, 개수>)
     */
    Map<RateLimitType, Long> countByType(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 최다 위반 IP 조회
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param limit 상위 N개
     * @return IP별 위반 횟수 리스트
     */
    List<Object[]> findTopViolatingIps(LocalDateTime startDate, LocalDateTime endDate, int limit);

    /**
     * 최다 위반 사용자 조회
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param limit 상위 N개
     * @return 사용자별 위반 횟수 리스트
     */
    List<Object[]> findTopViolatingUsers(LocalDateTime startDate, LocalDateTime endDate, int limit);
}

