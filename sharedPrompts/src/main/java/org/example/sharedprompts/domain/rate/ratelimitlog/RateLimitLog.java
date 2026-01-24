package org.example.sharedprompts.domain.rate.ratelimitlog;

import jakarta.persistence.*;
import lombok.*;
import org.example.sharedprompts.domain.rate.ratelimitlog.enums.RateLimitType;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.global.entity.BaseEntity;

/**
 * Rate Limit 로그 엔티티
 * Rate Limit 초과 이벤트를 기록하는 엔티티입니다.
 * 관리자가 Rate Limit 위반 패턴을 분석하고 모니터링할 수 있도록 합니다.
 */
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "rate_limit_logs", indexes = {
        @Index(name = "idx_rate_limit_log_user", columnList = "user_id"),
        @Index(name = "idx_rate_limit_log_ip", columnList = "client_ip"),
        @Index(name = "idx_rate_limit_log_rule", columnList = "rule_name"),
        @Index(name = "idx_rate_limit_log_created_at", columnList = "created_at"),
        @Index(name = "idx_rate_limit_log_user_created", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_rate_limit_log_ip_created", columnList = "client_ip, created_at DESC"),
        @Index(name = "idx_rate_limit_log_type_created", columnList = "rate_limit_type, created_at DESC")
})
public class RateLimitLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Rate Limit 규칙 이름
     */
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    /**
     * Rate Limit 키 값 (IP 또는 사용자 기반)
     * 키 값과 타입이 분리되어 저장되어 집계 쿼리 작성이 용이합니다.
     * 타입 정보는 rateLimitType 필드에 별도로 저장됩니다.
     */
    @Column(name = "rate_limit_key", nullable = false, length = 255)
    private String rateLimitKey;

    /**
     * 현재 요청 수
     */
    @Column(name = "current_count", nullable = false)
    private Long currentCount;

    /**
     * 허용된 최대 요청 수 (capacity)
     */
    @Column(name = "capacity", nullable = false)
    private Long capacity;

    /**
     * Retry-After 값 (초)
     */
    @Column(name = "retry_after", nullable = false)
    private Long retryAfter;

    /**
     * 사용자 ID (사용자 기반 Rate Limit인 경우)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * 클라이언트 IP 주소
     */
    @Column(name = "client_ip", nullable = false, length = 64)
    private String clientIp;

    /**
     * 요청 URI
     */
    @Column(name = "uri", nullable = false, length = 500)
    private String uri;

    /**
     * HTTP Method
     */
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    /**
     * Rate Limit 타입 (IP 또는 USER)
     * 키 값(rateLimitKey)과 분리되어 저장되어 타입별 집계 쿼리가 용이합니다.
     * 예: WHERE rate_limit_type = 'IP' GROUP BY ...
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rate_limit_type", nullable = false, length = 20)
    private RateLimitType rateLimitType;
}

