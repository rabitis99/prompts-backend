package org.example.sharedprompts.domain.rate.ratelimitlog.repository;

import org.example.sharedprompts.domain.rate.ratelimitlog.RateLimitLog;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Rate Limit 로그 Repository
 * 
 * JpaRepository를 상속하여 기본 CRUD 기능을 제공합니다.
 * Spring Data JPA의 메서드 네이밍 규칙을 활용한 간단한 조회 메서드를 포함합니다.
 * 복잡한 쿼리는 CustomRateLimitLogRepository에서 처리합니다.
 */
public interface RateLimitLogRepository extends JpaRepository<RateLimitLog, Long>, CustomRateLimitLogRepository {

    /**
     * 사용자 ID로 조회
     */
    Page<RateLimitLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * IP 주소로 조회
     */
    Page<RateLimitLog> findByClientIpOrderByCreatedAtDesc(String clientIp, Pageable pageable);

    /**
     * 규칙 이름으로 조회
     */
    Page<RateLimitLog> findByRuleNameOrderByCreatedAtDesc(String ruleName, Pageable pageable);

    /**
     * Rate Limit 타입으로 조회
     */
    Page<RateLimitLog> findByRateLimitTypeOrderByCreatedAtDesc(RateLimitType rateLimitType, Pageable pageable);
}

