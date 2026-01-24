package org.example.sharedprompts.auth.rate.policy;

import lombok.Getter;
import lombok.Setter;

/**
 * Rate Limit 실패 정책 설정
 * 
 * Rate limit 체크 실패 시 (Redis 장애, 인프라 문제 등)의 동작을 제어합니다.
 */
@Setter
@Getter
public class RateLimitFailurePolicy {
    /**
     * Fail-Open 정책 사용 여부
     * 
     * true: Rate limit 체크 실패 시 요청을 허용 (가용성 우선)
     * false: Rate limit 체크 실패 시 요청을 차단 (보안 우선)
     * 
     * 기본값: true (Fail-Open)
     * 
     * 보안 고려사항:
     * - Fail-Open은 Redis 장애 시 악의적인 트래픽이 통과할 수 있는 보안 리스크가 있습니다.
     * - 프로덕션 환경에서는 Fail-Closed(false)를 고려하거나, 
     *   별도의 Circuit Breaker 패턴과 함께 사용하는 것을 권장합니다.
     */
    private boolean failOpen = true;
}

