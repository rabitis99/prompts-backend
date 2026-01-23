package org.example.sharedprompts.domain.rate.ratelimitlog.repository.query;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Rate Limit 로그 Repository 쿼리 상수
 * 
 * 재사용되는 쿼리 문자열을 상수로 정의합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RateLimitLogQueries {

    /**
     * 오래된 로그 삭제 쿼리
     */
    public static final String DELETE_OLD_LOGS = 
            "DELETE FROM RateLimitLog r WHERE r.createdAt < :cutoffDate";

    /**
     * 시간대별 통계 조회 쿼리 (네이티브)
     */
    public static final String COUNT_BY_HOUR = 
            "SELECT EXTRACT(HOUR FROM r.created_at) as hour, COUNT(*) as count " +
            "FROM rate_limit_logs r " +
            "WHERE r.created_at >= :startDate AND r.created_at <= :endDate " +
            "GROUP BY EXTRACT(HOUR FROM r.created_at) " +
            "ORDER BY hour";

    /**
     * 규칙별 통계 조회 쿼리
     */
    public static final String COUNT_BY_RULE_NAME = 
            "SELECT r.ruleName, COUNT(r) " +
            "FROM RateLimitLog r " +
            "WHERE r.createdAt >= :startDate AND r.createdAt <= :endDate " +
            "GROUP BY r.ruleName " +
            "ORDER BY COUNT(r) DESC";

    /**
     * 타입별 통계 조회 쿼리
     */
    public static final String COUNT_BY_TYPE = 
            "SELECT r.rateLimitType, COUNT(r) " +
            "FROM RateLimitLog r " +
            "WHERE r.createdAt >= :startDate AND r.createdAt <= :endDate " +
            "GROUP BY r.rateLimitType";

    /**
     * 최다 위반 IP 조회 쿼리
     */
    public static final String FIND_TOP_VIOLATING_IPS = 
            "SELECT r.clientIp, COUNT(r) as violationCount " +
            "FROM RateLimitLog r " +
            "WHERE r.createdAt >= :startDate AND r.createdAt <= :endDate " +
            "GROUP BY r.clientIp " +
            "ORDER BY violationCount DESC";

    /**
     * 최다 위반 사용자 조회 쿼리
     */
    public static final String FIND_TOP_VIOLATING_USERS = 
            "SELECT r.user.id, COUNT(r) as violationCount " +
            "FROM RateLimitLog r " +
            "WHERE r.user IS NOT NULL " +
            "AND r.createdAt >= :startDate AND r.createdAt <= :endDate " +
            "GROUP BY r.user.id " +
            "ORDER BY violationCount DESC";
}






