package org.example.sharedprompts.auth.rate.filter.metrics;

import org.example.sharedprompts.auth.rate.RateLimiter;
import org.example.sharedprompts.auth.rate.policy.RateLimitRule;

/**
 * Rate Limit 메트릭 관찰자 인터페이스
 * 
 * Observer 패턴을 통해 Rate Limit 이벤트를 관찰하고 메트릭을 수집합니다.
 * 여러 관찰자를 등록하여 다양한 메트릭 수집 전략을 적용할 수 있습니다.
 */
public interface RateLimitMetricsObserver {

    /**
     * Rate Limit 체크 이벤트를 처리합니다.
     * 
     * @param rule RateLimitRule
     * @param result RateLimitResult
     */
    void onCheck(RateLimitRule rule, RateLimiter.RateLimitResult result);

    /**
     * Rate Limit 초과 이벤트를 처리합니다.
     * 
     * @param rule RateLimitRule
     * @param currentCount 현재 카운트
     * @param limit 제한 값
     */
    void onExceeded(RateLimitRule rule, long currentCount, long limit);

    /**
     * Rate Limit 체크 실패 이벤트를 처리합니다.
     * 
     * @param rule RateLimitRule
     */
    void onFailed(RateLimitRule rule);
}





